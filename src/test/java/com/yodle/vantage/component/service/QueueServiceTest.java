/*
 * Copyright 2016 Yodle, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yodle.vantage.component.service;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.yodle.vantage.component.dao.QueueDao;
import com.yodle.vantage.component.domain.Version;

//This suite does a lot of in-order verification to ensure that the queue gets locked at the appropriate time
 @RunWith(MockitoJUnitRunner.class)
public class QueueServiceTest {
    @InjectMocks private QueueService queueService;
    @Mock private ComponentService componentService;
    @Mock private QueueDao queueDao;

    @Test
    public void queueCreateRequest_locksTailBeforeSaving() {
        Version version = new Version("component", "version");

        queueService.queueCreateRequest(version);

        InOrder inOrder = Mockito.inOrder(queueDao);
        inOrder.verify(queueDao).lockQueueTail();
        inOrder.verify(queueDao).saveCreateRequest(version);
    }

    @Test
    public void givenEmptyQueue_processFrontOfQueue_locksHead() {
        when(queueDao.getCreateRequest()).thenReturn(Optional.empty());

        queueService.processFrontOfQueue();

        InOrder inOrder = Mockito.inOrder(queueDao);

        inOrder.verify(queueDao).lockQueueHead();
        inOrder.verify(queueDao).getCreateRequest();
        verifyNoMoreInteractions(queueDao, componentService);
    }

    @Test
    public void givenNonemptyQueue_processFrontOfQueue_processesInCorrectOrder() {
        Version version = new Version("component", "version");
        String requestId = "requestId";
        when(queueDao.getCreateRequest()).thenReturn(Optional.of(new QueueDao.QueueCreateRequest(version, requestId)));

        queueService.processFrontOfQueue();

        InOrder inOrder = Mockito.inOrder(queueDao, componentService);

        inOrder.verify(queueDao).lockQueueHead();
        inOrder.verify(componentService).createOrUpdateVersion(version);
        inOrder.verify(queueDao).lockQueueTail();
        inOrder.verify(queueDao).deleteCreateRequest(requestId);
    }


}