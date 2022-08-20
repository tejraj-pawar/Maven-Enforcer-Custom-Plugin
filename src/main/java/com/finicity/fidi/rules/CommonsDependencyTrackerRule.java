package com.finicity.fidi.rules;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.DependencyVersionMap;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommonsDependencyTrackerRule implements EnforcerRule
{
    private static Log log;

    private String ignoreDependency;

    private boolean uniqueVersion;

    public CommonsDependencyTrackerRule(String ignoreDependency, boolean uniqueVersion) {
        this.ignoreDependency = ignoreDependency;
        this.uniqueVersion = uniqueVersion;
    }

    public void execute( EnforcerRuleHelper helper) throws EnforcerRuleException
    {
        try
        {
            log = helper.getLog();
            log.info("CommonsDependencyTrackerRule running...");
            DependencyNode node = getNode( helper );
            DependencyVersionMap visitor = new DependencyVersionMap( log );
            visitor.setUniqueVersions( uniqueVersion );
            node.accept( visitor );
            List<CharSequence> errorMesssages = new ArrayList<CharSequence>();
            errorMesssages.addAll( getConvergenceErrorMsgs( visitor.getConflictedVersionNumbers() ) );
            if ( errorMesssages.size() > 0 )
            {
                for ( CharSequence errorMsg : errorMesssages )
                    log.warn( errorMsg );

                throw new EnforcerRuleException( "Detected more than one version of finicity-direct-integration-common dependency!!. "
                        + "See above detailed error message." );
            }
            log.info("CommonsDependencyTrackerRule successfully passed!!");
        }
        catch ( Exception e ) {
            throw new EnforcerRuleException( e.getLocalizedMessage(), e );
        }
    }

    private DependencyNode getNode(EnforcerRuleHelper helper ) throws EnforcerRuleException {
        try
        {
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            DependencyTreeBuilder dependencyTreeBuilder = helper.getComponent( DependencyTreeBuilder.class );
            ArtifactRepository repository = (ArtifactRepository) helper.evaluate( "${localRepository}" );
            ArtifactFilter filter = null; // we need to evaluate all scopes
            DependencyNode node = dependencyTreeBuilder.buildDependencyTree(project, repository, filter);
            return node;
        }
        catch ( ExpressionEvaluationException e ) {
            throw new EnforcerRuleException( "Unable to lookup an expression " + e.getLocalizedMessage(), e );
        }
        catch ( ComponentLookupException e ) {
            throw new EnforcerRuleException( "Unable to lookup a component " + e.getLocalizedMessage(), e );
        }
        catch ( DependencyTreeBuilderException e ) {
            throw new EnforcerRuleException( "Could not build dependency tree " + e.getLocalizedMessage(), e );
        }
    }

    private String getFullArtifactName( Artifact artifact ) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    private StringBuilder buildTreeString( DependencyNode node ) {
        List<String> loc = new ArrayList<String>();
        DependencyNode currentNode = node;
        while ( currentNode != null )
        {
            loc.add( getFullArtifactName( currentNode.getArtifact() ) );
            currentNode = currentNode.getParent();
        }
        Collections.reverse( loc );
        StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < loc.size(); i++ )
        {
            for ( int j = 0; j < i; j++ )
            {
                builder.append( "  " );
            }
            builder.append( "+-" + loc.get( i ) );
            builder.append( "\n" );
        }
        return builder;
    }

    private List<String> getConvergenceErrorMsgs( List<List<DependencyNode>> errors ) {
        List<String> errorMessages = new ArrayList<String>();
        for ( List<DependencyNode> nodeList : errors )
        {
            Artifact childNodeArtifact = nodeList.get(0).getArtifact();
            if(!isValidArtifactToTrack(childNodeArtifact)) {
                continue;
            }
            String errMsg = buildConvergenceErrorMsg( nodeList );
            if(StringUtils.isNotEmpty(errMsg))
                errorMessages.add( errMsg );
        }
        return errorMessages;
    }

    private boolean isValidArtifactToTrack(Artifact chilNodeArtifact) {
        if(chilNodeArtifact.getArtifactId().equalsIgnoreCase(ignoreDependency) &&
                chilNodeArtifact.getGroupId().equalsIgnoreCase(ignoreDependency)) {
            return true;
        }
        return false;
    }

    private String buildConvergenceErrorMsg( List<DependencyNode> nodeList )
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "\nDependency convergence error for " + getFullArtifactName( nodeList.get( 0 ).getArtifact() )
                + " paths to dependency are:\n" );
        if ( nodeList.size() > 0 ) {
            builder.append( buildTreeString( nodeList.get( 0 ) ) );
        }
        for ( DependencyNode node : nodeList.subList( 1, nodeList.size() ) ) {
            builder.append( "and\n" );
            builder.append( buildTreeString( node ) );
        }
        return builder.toString();
    }

    public String getCacheId() { return null; }

    public boolean isCacheable() { return false; }

    public boolean isResultValid( EnforcerRule rule ) { return false; }
}
