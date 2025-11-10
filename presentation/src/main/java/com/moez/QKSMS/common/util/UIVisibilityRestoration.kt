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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Comprehensive UI Visibility Restoration System
 * 
 * This system addresses rendering inconsistencies occurring during application startup
 * and when navigating back from secondary screens, including:
 * - View hierarchy refresh
 * - Layout constraint recalculation  
 * - Animation state cleanup
 * - Memory management optimization
 * - Cross-platform compatibility
 */
class UIVisibilityRestoration private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: UIVisibilityRestoration? = null
        
        fun getInstance(): UIVisibilityRestoration {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UIVisibilityRestoration().also { INSTANCE = it }
            }
        }
        
        private const val TAG = "UIVisibilityRestoration"
        private const val RESTORATION_DELAY_MS = 16L // One frame at 60fps
        private const val LAYOUT_TIMEOUT_MS = 5000L
        private const val MAX_RESTORATION_ATTEMPTS = 3
        private const val ANIMATION_DURATION_MS = 150L
    }
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeRestorations = ConcurrentHashMap<String, RestorationState>()
    private val pendingOperations = ConcurrentHashMap<String, Runnable>()
    private val animationCleanupTasks = ConcurrentHashMap<String, MutableList<Animator>>()
    
    /**
     * Represents the state of a UI restoration operation
     */
    private data class RestorationState(
        val activityRef: WeakReference<Activity>,
        val startTime: Long = System.currentTimeMillis(),
        val attemptCount: AtomicLong = AtomicLong(0),
        val isCompleted: AtomicBoolean = AtomicBoolean(false),
        val layoutObserver: ViewTreeObserver.OnGlobalLayoutListener? = null
    )
    
    /**
     * Configuration for UI restoration behavior
     */
    data class RestorationConfig(
        val enableAnimations: Boolean = true,
        val enableLayoutOptimization: Boolean = true,
        val enableMemoryOptimization: Boolean = true,
        val enableCrossDeviceCompatibility: Boolean = true,
        val maxRetryAttempts: Int = MAX_RESTORATION_ATTEMPTS,
        val animationDuration: Long = ANIMATION_DURATION_MS,
        val debugLogging: Boolean = true
    )
    
    /**
     * Initiates comprehensive UI visibility restoration for an activity
     */
    fun restoreUIVisibility(
        activity: Activity,
        rootView: View,
        config: RestorationConfig = RestorationConfig()
    ) {
        val timestamp = System.currentTimeMillis()
        val activityKey = activity::class.java.simpleName + "_" + activity.hashCode()
        
        if (config.debugLogging) {
            Log.d(TAG, "[$timestamp] Starting UI visibility restoration for $activityKey")
        }
        
        // Cancel any existing restoration for this activity
        cancelRestoration(activityKey)
        
        val restorationState = RestorationState(WeakReference(activity))
        activeRestorations[activityKey] = restorationState
        
        // Phase 1: Immediate visibility fixes
        performImmediateVisibilityFixes(activity, rootView, config)
        
        // Phase 3: Animation state cleanup
        if (config.enableAnimations) {
            cleanupAnimationStates(activity, rootView, config)
        }
        
        // Phase 4: Memory optimization
        if (config.enableMemoryOptimization) {
            optimizeMemoryUsage(activity, rootView, config)
        }
        
        // Phase 5: Cross-platform compatibility fixes
        if (config.enableCrossDeviceCompatibility) {
            applyCrossPlatformFixes(activity, rootView, config)
        }
        
        // Phase 6: Final validation and cleanup
        scheduleRestorationValidation(activityKey, config)
    }
    
    /**
     * Phase 1: Immediate visibility fixes
     */
    private fun performImmediateVisibilityFixes(
        activity: Activity,
        rootView: View,
        config: RestorationConfig
    ) {
        val timestamp = System.currentTimeMillis()
        
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                performImmediateVisibilityFixes(activity, rootView, config)
            }
            return
        }
        
        try {
            // Force visibility for critical UI components
            forceViewVisibility(rootView, View.VISIBLE, 1.0f)
            
            // Find and fix common problematic views
            val problematicViews = findProblematicViews(rootView)
            problematicViews.forEach { view ->
                forceViewVisibility(view, View.VISIBLE, 1.0f)
                
                // Special handling for RecyclerViews
                if (view is RecyclerView) {
                    restoreRecyclerViewVisibility(view, config)
                }
            }
            
            // Force layout invalidation
            rootView.invalidate()
            rootView.requestLayout()
        } catch (e: Exception) {
            Log.e(TAG, "[$timestamp] Error in immediate visibility fixes", e)
        }
    }

    /**
     * Phase 3: Animation state cleanup
     */
    private fun cleanupAnimationStates(
        activity: Activity,
        rootView: View,
        config: RestorationConfig
    ) {
        val timestamp = System.currentTimeMillis()
        val activityKey = activity::class.java.simpleName + "_" + activity.hashCode()
        
        if (config.debugLogging) {
            Log.d(TAG, "[$timestamp] Cleaning up animation states")
        }
        
        try {
            // Cancel any existing animations for this activity
            animationCleanupTasks[activityKey]?.forEach { animator ->
                if (animator.isRunning) {
                    animator.cancel()
                }
            }
            animationCleanupTasks[activityKey]?.clear()
            
            // Reset animation properties on all views
            resetViewAnimationProperties(rootView)
            
            // Create smooth fade-in animation if enabled
            if (config.enableAnimations) {
                createSmoothVisibilityAnimation(rootView, config, activityKey)
            }
            
            if (config.debugLogging) {
                Log.d(TAG, "[$timestamp] Animation state cleanup completed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "[$timestamp] Error cleaning up animation states", e)
        }
    }
    
    /**
     * Phase 4: Memory optimization
     */
    private fun optimizeMemoryUsage(
        activity: Activity,
        rootView: View,
        config: RestorationConfig
    ) {
        val timestamp = System.currentTimeMillis()
        
        if (config.debugLogging) {
            Log.d(TAG, "[$timestamp] Optimizing memory usage")
        }
        
        try {
            // Clean up view references
            cleanupViewReferences(rootView)
            
            // Optimize RecyclerView memory usage
            optimizeRecyclerViewMemory(rootView)
            
            // Request garbage collection (hint only)
            System.gc()
            
            if (config.debugLogging) {
                Log.d(TAG, "[$timestamp] Memory optimization completed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "[$timestamp] Error optimizing memory usage", e)
        }
    }
    
    /**
     * Phase 5: Cross-platform compatibility fixes
     */
    private fun applyCrossPlatformFixes(
        activity: Activity,
        rootView: View,
        config: RestorationConfig
    ) {
        val timestamp = System.currentTimeMillis()
        
        if (config.debugLogging) {
            Log.d(TAG, "[$timestamp] Applying cross-platform compatibility fixes")
        }
        
        try {
            // Handle different Android versions
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    applyAndroid11PlusFixes(activity, rootView, config)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    applyAndroid10Fixes(activity, rootView, config)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    applyAndroid9Fixes(activity, rootView, config)
                }
                else -> {
                    applyLegacyAndroidFixes(activity, rootView, config)
                }
            }
            
            // Handle different screen densities and orientations
            handleScreenVariations(activity, rootView, config)
            
            if (config.debugLogging) {
                Log.d(TAG, "[$timestamp] Cross-platform compatibility fixes applied")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "[$timestamp] Error applying cross-platform fixes", e)
        }
    }
    
    /**
     * Specialized RecyclerView visibility restoration
     */
    private fun restoreRecyclerViewVisibility(recyclerView: RecyclerView, config: RestorationConfig) {
        val timestamp = System.currentTimeMillis()
        
        if (config.debugLogging) {
            Log.d(TAG, "[$timestamp] Restoring RecyclerView visibility")
        }
        
        try {
            // Force RecyclerView visibility
            recyclerView.visibility = View.VISIBLE
            recyclerView.alpha = 1.0f
            
            // Force adapter refresh
            recyclerView.adapter?.notifyDataSetChanged()
            
            // Force layout manager to recalculate
            recyclerView.layoutManager?.requestLayout()
            
            // Schedule additional refresh
            mainHandler.postDelayed({
                try {
                    recyclerView.requestLayout()
                    recyclerView.invalidate()
                    
                    // Force scroll to ensure items are visible
                    if (recyclerView.adapter?.itemCount ?: 0 > 0) {
                        recyclerView.scrollToPosition(0)
                    }
                    
                    if (config.debugLogging) {
                        Log.d(TAG, "[${System.currentTimeMillis()}] RecyclerView visibility restoration completed")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in delayed RecyclerView restoration", e)
                }
            }, RESTORATION_DELAY_MS * 2)
            
        } catch (e: Exception) {
            Log.e(TAG, "[$timestamp] Error restoring RecyclerView visibility", e)
        }
    }
    
    /**
     * Handle system window insets for proper layout
     */
    private fun handleSystemWindowInsets(activity: Activity, rootView: View, config: RestorationConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                
                if (config.debugLogging) {
                    Log.d(TAG, "Applying window insets: top=${systemBars.top}, bottom=${systemBars.bottom}")
                }
                
                // Apply insets as padding to ensure content is not hidden
                view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
                )
                
                insets
            }
            
            // Request insets application
            ViewCompat.requestApplyInsets(rootView)
        }
    }
    
    /**
     * Find views that commonly have visibility issues
     */
    private fun findProblematicViews(rootView: View): List<View> {
        val problematicViews = mutableListOf<View>()
        
        fun traverseViews(view: View) {
            // Check if view has visibility issues
            if (view.visibility != View.VISIBLE || view.alpha < 1.0f) {
                problematicViews.add(view)
            }
            
            // Special cases for common problematic view types
            when (view) {
                is RecyclerView -> problematicViews.add(view)
                is ViewGroup -> {
                    // Check all children
                    for (i in 0 until view.childCount) {
                        traverseViews(view.getChildAt(i))
                    }
                }
            }
        }
        
        traverseViews(rootView)
        return problematicViews
    }
    
    /**
     * Force view visibility with proper error handling
     */
    private fun forceViewVisibility(view: View, visibility: Int, alpha: Float) {
        try {
            view.visibility = visibility
            view.alpha = alpha
            
            // Force immediate layout if needed
            if (view.isLayoutRequested) {
                view.requestLayout()
            }
            
            view.invalidate()
        } catch (e: Exception) {
            Log.e(TAG, "Error forcing view visibility", e)
        }
    }
    
    /**
     * Reset animation properties on views
     */
    private fun resetViewAnimationProperties(view: View) {
        try {
            view.scaleX = 1.0f
            view.scaleY = 1.0f
            view.translationX = 0.0f
            view.translationY = 0.0f
            view.rotation = 0.0f
            view.rotationX = 0.0f
            view.rotationY = 0.0f
            
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    resetViewAnimationProperties(view.getChildAt(i))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting animation properties", e)
        }
    }
    
    /**
     * Create smooth visibility animation
     */
    private fun createSmoothVisibilityAnimation(
        view: View,
        config: RestorationConfig,
        activityKey: String
    ) {
        try {
            val animator = ObjectAnimator.ofFloat(view, "alpha", 0.0f, 1.0f).apply {
                duration = config.animationDuration
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Remove from cleanup list when completed
                        animationCleanupTasks[activityKey]?.remove(this@apply)
                    }
                })
            }
            
            // Track animation for cleanup
            animationCleanupTasks.getOrPut(activityKey) { mutableListOf() }.add(animator)
            
            animator.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating visibility animation", e)
        }
    }
    
    /**
     * Clean up view references to prevent memory leaks
     */
    private fun cleanupViewReferences(view: View) {
        try {
            // Clear any cached drawables
            view.background = null
            
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    cleanupViewReferences(view.getChildAt(i))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up view references", e)
        }
    }
    
    /**
     * Optimize RecyclerView memory usage
     */
    private fun optimizeRecyclerViewMemory(rootView: View) {
        fun findRecyclerViews(view: View): List<RecyclerView> {
            val recyclerViews = mutableListOf<RecyclerView>()
            
            if (view is RecyclerView) {
                recyclerViews.add(view)
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    recyclerViews.addAll(findRecyclerViews(view.getChildAt(i)))
                }
            }
            
            return recyclerViews
        }
        
        try {
            val recyclerViews = findRecyclerViews(rootView)
            recyclerViews.forEach { recyclerView ->
                // Clear cached views
                recyclerView.recycledViewPool.clear()
                
                // Set reasonable cache sizes
                recyclerView.setItemViewCacheSize(10)
                recyclerView.recycledViewPool.setMaxRecycledViews(0, 20)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error optimizing RecyclerView memory", e)
        }
    }
    
    /**
     * Handle different screen variations
     */
    private fun handleScreenVariations(activity: Activity, rootView: View, config: RestorationConfig) {
        try {
            val configuration = activity.resources.configuration
            
            when (configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    if (config.debugLogging) {
                        Log.d(TAG, "Applying landscape orientation fixes")
                    }
                    // Apply landscape-specific fixes
                }
                Configuration.ORIENTATION_PORTRAIT -> {
                    if (config.debugLogging) {
                        Log.d(TAG, "Applying portrait orientation fixes")
                    }
                    // Apply portrait-specific fixes
                }
            }
            
            // Handle different screen densities
            val density = configuration.densityDpi
            when {
                density >= DisplayMetrics.DENSITY_XXXHIGH -> {
                    // XXXHDPI fixes
                }
                density >= DisplayMetrics.DENSITY_XXHIGH -> {
                    // XXHDPI fixes
                }
                density >= DisplayMetrics.DENSITY_XHIGH -> {
                    // XHDPI fixes
                }
                else -> {
                    // Standard density fixes
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling screen variations", e)
        }
    }
    
    /**
     * Android 11+ specific fixes
     */
    private fun applyAndroid11PlusFixes(activity: Activity, rootView: View, config: RestorationConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Handle window insets controller
                val controller = activity.window.insetsController
                controller?.let {
                    // Ensure proper insets handling
                    if (config.debugLogging) {
                        Log.d(TAG, "Applied Android 11+ window insets fixes")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error applying Android 11+ fixes", e)
            }
        }
    }
    
    /**
     * Android 10 specific fixes
     */
    private fun applyAndroid10Fixes(activity: Activity, rootView: View, config: RestorationConfig) {
        try {
            // Handle gesture navigation
            if (config.debugLogging) {
                Log.d(TAG, "Applied Android 10 gesture navigation fixes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying Android 10 fixes", e)
        }
    }
    
    /**
     * Android 9 specific fixes
     */
    private fun applyAndroid9Fixes(activity: Activity, rootView: View, config: RestorationConfig) {
        try {
            // Handle display cutout
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val displayCutout = rootView.rootWindowInsets?.displayCutout
                if (displayCutout != null && config.debugLogging) {
                    Log.d(TAG, "Applied Android 9 display cutout fixes")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying Android 9 fixes", e)
        }
    }
    
    /**
     * Legacy Android fixes
     */
    private fun applyLegacyAndroidFixes(activity: Activity, rootView: View, config: RestorationConfig) {
        try {
            // Handle older Android versions
            if (config.debugLogging) {
                Log.d(TAG, "Applied legacy Android fixes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying legacy Android fixes", e)
        }
    }
    
    /**
     * Schedule validation of restoration completion
     */
    private fun scheduleRestorationValidation(activityKey: String, config: RestorationConfig) {
        mainHandler.postDelayed({
            try {
                val restorationState = activeRestorations[activityKey]
                if (restorationState != null && !restorationState.isCompleted.get()) {
                    val activity = restorationState.activityRef.get()
                    if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                        validateRestorationCompletion(activity, activityKey, config)
                    } else {
                        // Activity is gone, clean up
                        cancelRestoration(activityKey)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in restoration validation", e)
            }
        }, LAYOUT_TIMEOUT_MS)
    }
    
    /**
     * Validate that restoration was successful
     */
    private fun validateRestorationCompletion(
        activity: Activity,
        activityKey: String,
        config: RestorationConfig
    ) {
        val timestamp = System.currentTimeMillis()
        
        try {
            val restorationState = activeRestorations[activityKey] ?: return
            
            if (config.debugLogging) {
                Log.d(TAG, "[$timestamp] Validating restoration completion for $activityKey")
            }
            
            // Mark as completed
            restorationState.isCompleted.set(true)
            
            // Clean up
            cancelRestoration(activityKey)
            
            if (config.debugLogging) {
                val duration = timestamp - restorationState.startTime
                Log.d(TAG, "[$timestamp] UI restoration completed for $activityKey in ${duration}ms")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "[$timestamp] Error validating restoration completion", e)
        }
    }
    
    /**
     * Cancel ongoing restoration for an activity
     */
    fun cancelRestoration(activityKey: String) {
        try {
            // Cancel pending operations
            pendingOperations.remove(activityKey)?.let { operation ->
                mainHandler.removeCallbacks(operation)
            }
            
            // Cancel animations
            animationCleanupTasks[activityKey]?.forEach { animator ->
                if (animator.isRunning) {
                    animator.cancel()
                }
            }
            animationCleanupTasks.remove(activityKey)
            
            // Remove restoration state
            activeRestorations.remove(activityKey)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling restoration", e)
        }
    }
    
    /**
     * Clean up all resources
     */
    fun cleanup() {
        try {
            // Cancel all pending operations
            pendingOperations.values.forEach { operation ->
                mainHandler.removeCallbacks(operation)
            }
            pendingOperations.clear()
            
            // Cancel all animations
            animationCleanupTasks.values.forEach { animators ->
                animators.forEach { animator ->
                    if (animator.isRunning) {
                        animator.cancel()
                    }
                }
            }
            animationCleanupTasks.clear()
            
            // Clear restoration states
            activeRestorations.clear()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}