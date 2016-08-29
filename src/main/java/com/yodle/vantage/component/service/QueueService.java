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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yodle.vantage.component.dao.QueueDao;
import com.yodle.vantage.component.domain.Version;

@Transactional
@Component
public class QueueService {
    @Autowired private QueueDao queueDao;
    @Autowired private ComponentService componentService;

    private static Logger l = LoggerFactory.getLogger(QueueService.class);

    public void queueCreateRequest(Version version) {
        l.info("Saving create request for [{}]:[{}]", version.getComponent(), version.getVersion());
        //serialize adding things to the end of the queue
        queueDao.lockQueueTail();
        queueDao.saveCreateRequest(version);
    }

    public void processFrontOfQueue() {
        queueDao.lockQueueHead();
        Optional<QueueDao.QueueCreateRequest> createRequest = queueDao.getCreateRequest();

        createRequest.map(qcr -> {
            l.info("Processing create queue entry for {}:{}", qcr.v.getComponent(), qcr.v.getVersion());
            componentService.createOrUpdateVersion(qcr.v);
            //strictly speaking not always necessary, but safe to do in case the head of the queue is also the tail
            //and deleting the node we just processed interferes with en1queuing a new node
            queueDao.lockQueueTail();
            queueDao.deleteCreateRequest(qcr.id);
            l.info("Processed create queue entry for {}:{}", qcr.v.getComponent(), qcr.v.getVersion());
            return null;
        });
    }
}
