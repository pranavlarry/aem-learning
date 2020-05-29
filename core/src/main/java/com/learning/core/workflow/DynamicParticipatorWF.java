package com.learning.core.workflow;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.ParticipantStepChooser;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.learning.core.service.CustomEmail;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.*;

@Component(service= ParticipantStepChooser.class,property = {"chooser.label=Sample Implementation of dynamic participant chooser"})
public class DynamicParticipatorWF implements ParticipantStepChooser {

    private Map<String ,Map<String,String>> projectGrpMap= new HashMap<>();


    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private CustomEmail customEmail;

    @Reference
    private ConfigurationAdmin configurationAdmin;

    private ResourceResolver resolver = null;

    private final String TEMPLATE_PATH = "/apps/learning/workflow/email/approveReq-template.txt";


    @Override
    public String getParticipant(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        String participant = null;
        String path = workItem.getWorkflowData().getPayload().toString();
        String parentPage = path.split("/",5)[2];
        String pid="com."+parentPage+".usergroups";

        String type = null;

        if(metaDataMap.containsKey("PROCESS_ARGS")) {
            type = metaDataMap.get("PROCESS_ARGS",String.class).split("=")[1];
            if(type == null) {
                throw new NullPointerException("Type not specified");
            }
        }

        try {
            Configuration conf = configurationAdmin.getConfiguration(pid);
            Dictionary<String,Object> properties = conf.getProperties();

            participant = properties.get(type).toString();
            if(participant == null) {
                throw new NullPointerException("Can't Find the type of group or member ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        resolver = workItem.getMetaDataMap().get("resolver",ResourceResolver.class);

        List<String> toIds = new ArrayList<>();


        UserManager manager = resolver.adaptTo(UserManager.class);

        Authorizable authorAdminAuthorizable = null;
        Iterator authorAdmin = null;
        try {
            authorAdminAuthorizable = manager.getAuthorizable(participant);
            Group authorAdminGroup = (Group) authorAdminAuthorizable;
            authorAdmin = authorAdminGroup.getMembers();
            while(authorAdmin.hasNext()) {
                Object obj = authorAdmin.next();
                if(obj instanceof User) {
                    User user = (User) obj;
                    Authorizable userAuthorization = null;
                    userAuthorization = manager.getAuthorizable(user.getID());
                    if(userAuthorization.hasProperty("profile/email")) {
                        if(!userAuthorization.getProperty("profile/email").equals("")) {
                            toIds.add(PropertiesUtil.toString(userAuthorization.getProperty("profile/email"),""));
                        }
                    }
                }
            }
        }catch (RepositoryException e) {
            e.printStackTrace();
        }

        Map<String,String> params = new HashMap<>();

        try {
            Authorizable authorizable = manager.getAuthorizable(workItem.getWorkflow().getInitiator());
            String initiator = authorizable.getID();
            params.put("url","localhost:4502"+workItem.getWorkflowData().getPayload().toString());
            params.put("initiator",initiator);
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

        String[] ids = Arrays.copyOf(toIds.toArray(), toIds.toArray().length, String[].class);

        customEmail.sendEmail(TEMPLATE_PATH, ids ,params,resolver);

        return participant;
    }


}
