package com.finicity.fidi;

import com.finicity.fidi.rules.CommonsDependencyTrackerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.enforcer.DefaultEnforcementRuleHelper;
import org.apache.maven.plugins.enforcer.EnforcerExpressionEvaluator;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;

// goal name is "track-commons-version"
@Mojo(name = "track-commons-version", defaultPhase = LifecyclePhase.COMPILE)
public class TrackCommonsVersionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(required = true, defaultValue = "finicity-direct-integration-common")
    String ignoreDependency;

    @Parameter(required = true, defaultValue = "true")
    boolean uniqueVersion;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    MavenSession session;

    @Parameter( defaultValue = "${mojoExecution}", readonly = true, required = true )
    MojoExecution mojoExecution;

    PlexusContainer container;

    public void contextualize( Context context )
            throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void execute() throws MojoExecutionException {
        Log log = this.getLog();

        EnforcerExpressionEvaluator evaluator =
                new EnforcerExpressionEvaluator( session, mojoExecution );

        EnforcerRuleHelper helper =
                new DefaultEnforcementRuleHelper( session, evaluator, log, container );

        CommonsDependencyTrackerRule rule = new CommonsDependencyTrackerRule(ignoreDependency, uniqueVersion);
        try {
            rule.execute(helper);
        } catch (EnforcerRuleException e) {
            throw new MojoExecutionException( "track-commons-version-maven-plugin rule failed: " + e.getMessage(), e );
        }


    }
}
