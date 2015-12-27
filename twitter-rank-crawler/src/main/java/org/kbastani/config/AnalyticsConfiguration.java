package org.kbastani.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.transaction.Transaction;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

/**
 * This configuration defines the setup information for RabbitMQ queues and a command line runner bean
 * that will ensure that a unique constraint is applied to the connected Neo4j database server
 *
 * @author kbastani
 */
@Configuration
public class AnalyticsConfiguration {

    private final Log logger = LogFactory.getLog(AnalyticsConfiguration.class);

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        return new TomcatEmbeddedServletContainerFactory();
    }

    @Bean
    Queue profiles() {
        return new Queue("twitter.profiles", true, true, true);
    }

    @Bean
    Queue follows() {
        return new Queue("twitter.follows", true, true, true);
    }

    @Bean
    Queue followers() {
        return new Queue("twitter.followers", true, true, true);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange("twitter.exchange", true, true);
    }

    @Bean
    Twitter twitter(final @Value("${spring.social.twitter.appId}") String appId,
                    final @Value("${spring.social.twitter.appSecret}") String appSecret,
                    final @Value("${spring.social.twitter.accessToken}") String accessToken,
                    final @Value("${spring.social.twitter.accessTokenSecret}") String accessTokenSecret) {
        return new TwitterTemplate(appId, appSecret, accessToken, accessTokenSecret);
    }

    @Bean
    CommandLineRunner commandLineRunner(Neo4jSession neo4jSession) {
        return (args) -> {
            // Make sure that a constraint is created on the Neo4j database
            try {
                // This constraint ensures that each profileId is unique per user node
                Transaction tx = neo4jSession.beginTransaction();
                neo4jSession.execute("CREATE CONSTRAINT ON (user:User) ASSERT user.profileId IS UNIQUE");
                tx.commit();
                tx.close();
            } catch (Exception ex) {
                // The constraint is already created or the database is not available
                logger.error(ex);
            }

        };
    }
}