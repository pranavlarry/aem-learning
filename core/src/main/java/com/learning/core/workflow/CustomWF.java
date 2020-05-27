package com.learning.core.workflow;

import com.day.cq.workflow.exec.HistoryItem;
import com.day.cq.workflow.exec.Workflow;
import com.learning.core.service.CustomEmail;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.workflow.WorkflowException;

import com.day.cq.workflow.WorkflowSession;

import com.day.cq.workflow.exec.WorkItem;

import com.day.cq.workflow.exec.WorkflowProcess;

import com.day.cq.workflow.metadata.MetaDataMap;

import java.util.*;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;


@Component(service = WorkflowProcess.class,
        immediate = true, enabled = true,
        property = {"process.label= Custom Email"})
public class CustomWF implements WorkflowProcess
{

    @Reference
    private CustomEmail customEmail;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String templatePath = null;

    public void execute(WorkItem item, WorkflowSession wfsession,MetaDataMap args) throws WorkflowException {


        ResourceResolver resourceResolver = null;

        Map<String,String> params = new HashMap<>();

        String type = null;
        if(args.containsKey("PROCESS_ARGS")) {
            type = args.get("PROCESS_ARGS",String.class).split("=")[1];
        }

        String participant = null;

        Workflow wf = item.getWorkflow();
        List<HistoryItem> historyList = wfsession.getHistory(wf);

        String comment = null;

        HistoryItem current = historyList.get(historyList.size() - 1);

        if(type.equalsIgnoreCase("approved") || type.equalsIgnoreCase("reject")) {
            do {
                if(!current.getComment().equals("")) {
                    comment = current.getComment();
                    participant = current.getUserId();
                    break;
                }
                current = current.getPreviousHistoryItem();
            }while(current != null);
        }

        switch (type.toLowerCase()) {
            case "publish to dev":
                this.templatePath = "/apps/learning/workflow/email/publishDev.txt";
                break;
            case "approved":
                this.templatePath = "/apps/learning/workflow/email/approveOrReject.txt";
                params.put("approveOrReject","approved");
                params.put("comments",comment);
                params.put("participant",participant);
                break;
            case "reject":
                this.templatePath = "/apps/learning/workflow/email/approveOrReject.txt";
                params.put("approveOrReject","rejected");
                params.put("comments",comment);
                params.put("participant",participant);
                break;
            case "complete":
                this.templatePath = "/apps/learning/workflow/email/completed.txt";
                break;
        }

        try
        {

            resourceResolver = resourceResolverFactory.getResourceResolver(Collections.<String, Object>singletonMap(JcrResourceConstants.AUTHENTICATION_INFO_SESSION,
                    wfsession.getSession()));

            UserManager manager = resourceResolver.adaptTo(UserManager.class);

            Authorizable authorizable = manager.getAuthorizable(item.getWorkflow().getInitiator());

            String userEmail = PropertiesUtil.toString(authorizable.getProperty("profile/email"), "");


            params.put("recipientName",userEmail);

            customEmail.sendEmail(templatePath, new String[]{userEmail},params,resourceResolver);


        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
