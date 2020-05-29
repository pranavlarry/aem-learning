package com.learning.core.serviceImpl;

import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.learning.core.service.CustomEmail;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Map;

@Component(service = CustomEmail.class,immediate = true)
public class CustomEmailImpl implements CustomEmail {

    @Reference
    private MessageGatewayService messageGatewayService;

    @Override
    public void sendEmail(String templatePath, String[] toIds, Map params,ResourceResolver resolver) {
        try {

            Node templateNode = resolver.getResource(templatePath).adaptTo(Node.class);
            final MailTemplate mailTemplate = MailTemplate.create(templatePath, templateNode.getSession());
            HtmlEmail email = mailTemplate.getEmail(StrLookup.mapLookup(params), HtmlEmail.class);
            email.addTo(toIds);
            MessageGateway<HtmlEmail> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);
            messageGateway.send(email);


        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        } catch (EmailException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
