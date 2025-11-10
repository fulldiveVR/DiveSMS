/*
 * Copyright (C) 2024 DiveSMS Project
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.moez.QKSMS.common.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * State Update Throttler
 * 
 * Prevents rapid successive state updates that can cause UI rendering conflicts.
 * Implements intelligent batching and throttling of state changes.
 */
class StateUpdateThrottler private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: StateUpdateThrottler? = null
        
        fun getInstance(): StateUpdateThrottler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StateUpdateThrottler().also { INSTANCE = it }
            }
        }
        
        private const val TAG = "StateUpdateThrottler"
        private const val DEFAULT_THROTTLE_DELAY_MS = 16L // One frame at 60fps
        private const val MAX_BATCH_SIZE = 10
        private const val BATCH_TIMEOUT_MS = 100L
    }
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingUpdates = ConcurrentHashMap<String, PendingUpdate>()
    private val updateCounters = ConcurrentHashMap<String, AtomicLong>()
    
    /**
     * Represents a pending state update
     */
    private data class PendingUpdate(
        val key: String,
        val updates: MutableList<() -> Unit> = mutableListOf(),
        val firstUpdateTime: Long = System.currentTimeMillis(),
        val isScheduled: AtomicBoolean = AtomicBoolean(false)
    )
    
    /**
     * Configuration for throttling behavior
     */
    data class ThrottleConfig(
        val throttleDelayMs: Long = DEFAULT_THROTTLE_DELAY_MS,
        val maxBatchSize: Int = MAX_BATCH_SIZE,
        val batchTimeoutMs: Long = BATCH_TIMEOUT_MS,
        val enableBatching: Boolean = true,
        val enableLogging: Boolean = false
    )
    
    /**
     * Throttle a state update operation
     */
    fun throttleUpdate(
        key: String,
        update: () -> Unit,
        config: ThrottleConfig = ThrottleConfig()
    ) {
        val timestamp = System.currentTimeMillis()
        
        if (config.enableLogging) {
            Log.d(TAG, "[$timestamp] Throttling update for key: $key")
        }
        
        // Increment update counter
        val counter = updateCounters.getOrPut(key) { AtomicLong(0) }
        val updateCount = counter.incrementAndGet()
        
        if (config.enableBatching) {
            batchUpdate(key, update, config, updateCount)
        } else {
            singleUpdate(key, update, config, updateCount)
        }
    }
    
    /**
     * Handle batched updates
     */
    private fun batchUpdate(
        key: String,
        update: () -> Unit,
        config: ThrottleConfig,
        updateCount: Long
    ) {
        val pendingUpdate = pendingUpdates.getOrPut(key) { PendingUpdate(key) }
        
        synchronized(pendingUpdate) {
            // Add update to batch
            pendingUpdate.updates.add(update)
            
            val shouldExecuteImmediately = when {
                // Execute immediately if batch is full
                pendingUpdate.updates.size >= config.maxBatchSize -> true
                // Execute immediately if batch timeout exceeded
                System.currentTimeMillis() - pendingUpdate.firstUpdateTime > config.batchTimeoutMs -> true
                else -> false
            }
            
            if (shouldExecuteImmediately) {
                executeBatch(key, pendingUpdate, config)
            } else if (!pendingUpdate.isScheduled.get()) {
                scheduleBatchExecution(key, pendingUpdate, config)
            }
        }
        
        if (config.enableLogging) {
            Log.d(TAG, "Batched update #$updateCount for key: $key, batch size: ${pendingUpdate.updates.size}")
        }
    }
    
    /**
     * Handle single throttled update
     */
    private fun singleUpdate(
        key: String,
        update: () -> Unit,
        config: ThrottleConfig,
        updateCount: Long
    ) {
        val pendingUpdate = pendingUpdates.getOrPut(key) { PendingUpdate(key) }
        
        synchronized(pendingUpdate) {
            // Replace previous update with latest
            pendingUpdate.updates.clear()
            pendingUpdate.updates.add(update)
            
            if (!pendingUpdate.isScheduled.get()) {
                scheduleUpdateExecution(key, pendingUpdate, config)
            }
        }
        
        if (config.enableLogging) {
            Log.d(TAG, "Throttled single update #$updateCount for key: $key")
        }
    }
    
    /**
     * Schedule batch execution
     */
    private fun scheduleBatchExecution(
        key: String,
        pendingUpdate: PendingUpdate,
        config: ThrottleConfig
    ) {
        if (pendingUpdate.isScheduled.compareAndSet(false, true)) {
            mainHandler.postDelayed({
                executeBatch(key, pendingUpdate, config)
            }, config.throttleDelayMs)
        }
    }
    
    /**
     * Schedule single update execution
     */
    private fun scheduleUpdateExecution(
        key: String,
        pendingUpdate: PendingUpdate,
        config: ThrottleConfig
    ) {
        if (pendingUpdate.isScheduled.compareAndSet(false, true)) {
            mainHandler.postDelayed({
                executeUpdate(key, pendingUpdate, config)
            }, config.throttleDelayMs)
        }
    }
    
    /**
     * Execute batched updates
     */
    private fun executeBatch(
        key: String,
        pendingUpdate: PendingUpdate,
        config: ThrottleConfig
    ) {
        val timestamp = System.currentTimeMillis()
        
        try {
            val updatesToExecute = synchronized(pendingUpdate) {
                val updates = pendingUpdate.updates.toList()
                pendingUpdate.updates.clear()
                pendingUpdate.isScheduled.set(false)
                updates
            }
            
            if (updatesToExecute.isNotEmpty()) {
                if (config.enableLogging) {
                    Log.d(TAG, "[$timestamp] Executing batch of ${updatesToExecute.size} updates for key: $key")
                }
                
                // Execute all updates in batch
                updatesToExecute.forEach { update ->
                    try {
                        update()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing batched update for key: $key", e)
                    }
                }
                
                if (config.enableLogging) {
                    Log.d(TAG, "[$timestamp] Batch execution completed for key: $key")
                }
            }
            
            // Clean up if no more updates pending
            if (pendingUpdate.updates.isEmpty()) {
                pendingUpdates.remove(key)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "[$timestamp] Error executing batch for key: $key", e)
            pendingUpdate.isScheduled.set(false)
        }
    }
    
    /**
     * Execute single update
     */
    private fun executeUpdate(
        key: String,
        pendingUpdate: PendingUpdate,
        config: ThrottleConfig
    ) {
        val timestamp = System.currentTimeMillis()
        
        try {
            val updateToExecute = synchronized(pendingUpdate) {
                val update = pendingUpdate.updates.lastOrNull()
                pendingUpdate.updates.clear()
                pendingUpdate.isScheduled.set(false)
                update
            }
            
            updateToExecute?.let { update ->
                if (config.enableLogging) {
                    Log.d(TAG, "[$timestamp] Executing throttled update for key: $key")
                }
                
                try {
                    update()
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing throttled update for key: $key", e)
                }
                
                if (config.enableLogging) {
                    Log.d(TAG, "[$timestamp] Throttled update completed for key: $key")
                }
            }
            
            // Clean up if no more updates pending
            if (pendingUpdate.updates.isEmpty()) {
                pendingUpdates.remove(key)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "[$timestamp] Error executing update for key: $key", e)
            pendingUpdate.isScheduled.set(false)
        }
    }
    
    /**
     * Force immediate execution of pending updates for a key
     */
    fun flushUpdates(key: String, config: ThrottleConfig = ThrottleConfig()) {
        val timestamp = System.currentTimeMillis()
        
        if (config.enableLogging) {
            Log.d(TAG, "[$timestamp] Flushing updates for key: $key")
        }
        
        val pendingUpdate = pendingUpdates[key]
        if (pendingUpdate != null) {
            // Cancel scheduled execution
            pendingUpdate.isScheduled.set(false)
            
            // Execute immediately
            if (config.enableBatching) {
                executeBatch(key, pendingUpdate, config)
            } else {
                executeUpdate(key, pendingUpdate, config)
            }
        }
    }
    
    /**
     * Force immediate execution of all pending updates
     */
    fun flushAllUpdates(config: ThrottleConfig = ThrottleConfig()) {
        val timestamp = System.currentTimeMillis()
        
        if (config.enableLogging) {
            Log.d(TAG, "[$timestamp] Flushing all pending updates")
        }
        
        val keysToFlush = pendingUpdates.keys.toList()
        keysToFlush.forEach { key ->
            flushUpdates(key, config)
        }
    }
    
    /**
     * Cancel pending updates for a key
     */
    fun cancelUpdates(key: String) {
        val pendingUpdate = pendingUpdates.remove(key)
        if (pendingUpdate != null) {
            synchronized(pendingUpdate) {
                pendingUpdate.updates.clear()
                pendingUpdate.isScheduled.set(false)
            }
        }
        updateCounters.remove(key)
    }
    
    /**
     * Cancel all pending updates
     */
    fun cancelAllUpdates() {
        pendingUpdates.keys.toList().forEach { key ->
            cancelUpdates(key)
        }
    }
    
    /**
     * Get statistics about pending updates
     */
    fun getStatistics(): Map<String, Int> {
        return pendingUpdates.mapValues { (_, pendingUpdate) ->
            synchronized(pendingUpdate) {
                pendingUpdate.updates.size
            }
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            cancelAllUpdates()
            pendingUpdates.clear()
            updateCounters.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}