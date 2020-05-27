package com.learning.core.workflow;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Collections;

@Component(service = WorkflowProcess.class,
        immediate = true, enabled = true,
        property = {"process.label= Get Resolver"})
public class GetResolverWF implements WorkflowProcess {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    ResourceResolver resolver;
    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        try {
            resolver = resourceResolverFactory.getResourceResolver(Collections.<String, Object>singletonMap(JcrResourceConstants.AUTHENTICATION_INFO_SESSION,
                    workflowSession.getSession()));
        } catch (LoginException e) {
            e.printStackTrace();
        }

        workItem.getMetaDataMap().put("resolver",resolver);
    }
}
