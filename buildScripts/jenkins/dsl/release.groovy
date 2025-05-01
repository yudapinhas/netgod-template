pipelineJob('pulumi-komponents-release') {
    def repo = 'https://github.com/kenshoo/pulumi-komponents'
    def sshRepo = 'git@github.com:kenshoo/pulumi-komponents.git'
    logRotator {
        numToKeep(30)
    }
    description("Pulumi KS provision Automation - Pipeline")

    properties{
        githubProjectUrl (repo)
    }

    parameters {
        stringParam('COMMIT_ID', null, 'The commit sha to build')
    }
    triggers {
        githubPush()
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(sshRepo)
                        credentials('kgithub-build-jenkins-core-key')
                    }
                    branches('master')
                    scriptPath('buildScripts/jenkins/pipelines/release.groovy')
                }
            }
        }
    }
}
