package com.learning.core.workflow;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.AssetReferenceSearch;
import com.day.cq.replication.*;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Node;
import java.util.*;


@Component(service = WorkflowProcess.class,
        immediate = true,
        property = {"process.label = Custom Replication"})
public class ReplicationWF implements WorkflowProcess {
    @Reference
    private ResourceResolverFactory resolverFactory;

    private ResourceResolver resolver=null;

    @Reference
    private Replicator replicator;

    private String agentId;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {



        String path = workItem.getWorkflowData().getPayload().toString();
        if(metaDataMap.containsKey("PROCESS_ARGS")) {
            this.agentId = metaDataMap.get("PROCESS_ARGS",String.class);
            this.agentId = agentId.split("=")[1];
        }

        try {
            final Map<String,Object> params = new HashMap<>();

            resolver = resolverFactory.getResourceResolver(Collections.singletonMap("user.jcr.session",workflowSession.getSession()));
            PageManager pageManager = resolver.adaptTo(PageManager.class);
            Page page = pageManager.getPage(path);

            replicateTemplate(path,workflowSession,page);

            replicateAssets(path,workflowSession,page);

            replicateContent(path,workflowSession);

        } catch (ReplicationException | LoginException e) {
            e.printStackTrace();
        }


    }

    private void replicateTemplate(String path, WorkflowSession wfSession, Page page) throws ReplicationException {
        String template = page.getTemplate().getPath();
        replicateContent(template,wfSession);
    }

    private void replicateAssets(String path, WorkflowSession wfSession, Page page) throws ReplicationException {
        Set<String> assetsPath = getAssetsPath(path,page);

        if(assetsPath.size() == 0) {
            return;
        }

        for(String assetPath : assetsPath) {
            replicateContent(assetPath,wfSession);
        }
    }

    private Set getAssetsPath(String path,Page page) {

        if(page == null) {
            return new LinkedHashSet();
        }

        Resource resource = page.getContentResource();

        AssetReferenceSearch assetReferenceSearch = new AssetReferenceSearch(resource.adaptTo(Node.class), DamConstants.MOUNTPOINT_ASSETS,resolver);

        Map <String, Asset> assetMap = assetReferenceSearch.search();

        return assetMap.keySet();
    }

    private void replicateContent(String path,WorkflowSession wfSession) throws ReplicationException {

        // Create leanest replication options for activation
        ReplicationOptions options = new ReplicationOptions();
        // Do not create new versions as this adds to overhead
        options.setSuppressVersions(true);
        // Avoid sling job overhead by forcing synchronous. Note this will result in serial activation.
        options.setSynchronous(true);
        // Do NOT suppress status update of resource (set replication properties accordingly)
        options.setSuppressStatusUpdate(false);

        options.setFilter(new AgentFilter() {
            @Override
            public boolean isIncluded(Agent agent) {
                return agentId.equalsIgnoreCase(agent.getId());
            }
        });

        replicator.replicate(wfSession.getSession(), ReplicationActionType.ACTIVATE, path, options);
    }
}
