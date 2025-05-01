pipelineJob('pulumi-komponents-pull-request') {
    def repo = 'https://github.com/kenshoo/pulumi-komponents'
    def sshRepo = 'git@github.com:kenshoo/pulumi-komponents.git'
    logRotator {
        numToKeep(30)
    }
    description("PR Pulumi KS provision Automation - Pipeline")
    properties{
        githubProjectUrl (repo)
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(sshRepo)
                        credentials('kgithub-build-jenkins-core-key')
                        refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                    }
                    branches('${sha1}')
                    scriptPath('buildScripts/jenkins/pipelines/pull_request.groovy')
                }
            }
            triggers {
                githubPullRequest {
                    orgWhitelist('Kenshoo')
                    useGitHubHooks()
                    extensions {
                        commitStatus {
                            context("PR Pulumi components provision Automation")
                        }
                    }
                }
            }
        }
    }
}
