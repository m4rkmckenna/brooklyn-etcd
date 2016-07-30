/*
 * Copyright 2014-2016 by Cloudsoft Corporation Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.brooklyn.entity.nosql.etcd;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.core.config.render.RendererHints;
import org.apache.brooklyn.entity.software.base.SoftwareProcessImpl;
import org.apache.brooklyn.entity.stock.DelegateEntity;
import org.apache.brooklyn.util.text.Strings;

public class EtcdNodeImpl extends SoftwareProcessImpl implements EtcdNode {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdNode.class);

    public void init() {
       super.init();

       String nodeName = config().get(ETCD_NODE_NAME);
       Entity cluster = getParent();

       if (cluster instanceof EtcdCluster) {
           sensors().set(ETCD_CLUSTER, cluster);
           String clusterName = cluster.config().get(EtcdCluster.CLUSTER_NAME);
           if (Strings.isBlank(nodeName)) nodeName = clusterName;
           AtomicInteger nodeId = cluster.sensors().get(EtcdCluster.NODE_ID);
           nodeName += nodeId.incrementAndGet();
       } else {
           sensors().set(EtcdNode.ETCD_NODE_HAS_JOINED_CLUSTER, Boolean.TRUE);
       }

       sensors().set(ETCD_NODE_NAME, Strings.isBlank(nodeName) ? getId() : nodeName);
       sensors().set(CLIENT_SCHEME, getClientProtocol());
       LOG.info("Starting {} node: {}", cluster instanceof EtcdCluster ? "clustered" : "single", getNodeName());
    }

    @Override
    public String getNodeName() {
        return sensors().get(EtcdNode.ETCD_NODE_NAME);
    }

    @Override
    public String getClientProtocol() {
        return getProtocol(config().get(EtcdNode.SECURE_CLIENT));
    }

    @Override
    public String getPeerProtocol() {
        return getProtocol(config().get(EtcdNode.SECURE_PEER));
    }

    @Override
    public Integer getClientPort() {
        return sensors().get(EtcdNode.ETCD_CLIENT_PORT);
    }

    @Override
    public Integer getPeerPort() {
        return sensors().get(EtcdNode.ETCD_PEER_PORT);
    }

    @Override
    public String getClusterToken() {
        return config().get(EtcdCluster.CLUSTER_TOKEN);
    }

    @Override
    public EtcdNodeDriver getDriver() {
        return (EtcdNodeDriver) super.getDriver();
    }

    @Override
    public Class<? extends EtcdNodeDriver> getDriverInterface() {
        return EtcdNodeDriver.class;
    }

    @Override
    public void connectSensors() {
        super.connectSensors();
        connectServiceUpIsRunning();
    }

    @Override
    public void disconnectSensors() {
        disconnectServiceUpIsRunning();
        super.disconnectSensors();
    }

    @Override
    public void joinCluster(String nodeName, String nodeAddress) {
        getDriver().joinCluster(nodeName, nodeAddress);
    }

    @Override
    public void leaveCluster(String nodeName) {
        getDriver().leaveCluster(nodeName);
    }

    @Override
    public boolean hasJoinedCluster() {
        return Boolean.TRUE.equals(sensors().get(EtcdNode.ETCD_NODE_HAS_JOINED_CLUSTER));
    }

    protected String getProtocol(Boolean secure) {
        return Boolean.TRUE.equals(secure) ? "https" : "http";
    }

    static {
        RendererHints.register(ETCD_CLUSTER, RendererHints.openWithUrl(DelegateEntity.EntityUrl.entityUrl()));
    }

}
