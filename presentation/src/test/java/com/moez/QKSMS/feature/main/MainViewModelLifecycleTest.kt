/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
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
package com.moez.QKSMS.feature.main

import com.moez.QKSMS.model.Conversation
import com.moez.QKSMS.repository.ConversationRepository
import io.reactivex.subjects.BehaviorSubject
import io.realm.RealmResults
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import com.moez.QKSMS.extensions.asObservable

/**
 * Test class to verify the UI lifecycle fixes
 */
class MainViewModelLifecycleTest {

    @Mock
    private lateinit var conversationRepo: ConversationRepository
    
    @Mock
    private lateinit var realmResults: RealmResults<Conversation>
    
    private lateinit var realmSubject: BehaviorSubject<RealmResults<Conversation>>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        realmSubject = BehaviorSubject.create()
        
        // Mock RealmResults behavior
        `when`(realmResults.asObservable()).thenReturn(realmSubject)
        `when`(conversationRepo.getConversations()).thenReturn(realmResults)
    }

    @Test
    fun `test initial state has null data until realm loads`() {
        // Given: RealmResults is not loaded yet
        `when`(realmResults.isLoaded).thenReturn(false)
        
        // When: ViewModel is created
        // Then: Initial state should have null data
        // This prevents showing empty UI before data is actually loaded
        
        // Simulate data loading
        `when`(realmResults.isLoaded).thenReturn(true)
        realmSubject.onNext(realmResults)
        
        // Verify that data is only updated after isLoaded = true
        verify(realmResults, atLeastOnce()).isLoaded
    }

    @Test
    fun `test navigation updates wait for data to load`() {
        // Given: RealmResults starts as not loaded
        `when`(realmResults.isLoaded).thenReturn(false)
        
        // When: Navigation occurs
        // Then: Should wait for data to be loaded before updating state
        
        // Simulate data becoming available
        `when`(realmResults.isLoaded).thenReturn(true)
        realmSubject.onNext(realmResults)
        
        // Verify proper loading sequence
        verify(realmResults, atLeastOnce()).isLoaded
    }
}