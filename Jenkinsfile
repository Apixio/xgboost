#!/usr/bin/groovy
// -*- mode: groovy -*-
// Jenkins pipeline
// See documents at https://jenkins.io/doc/book/pipeline/jenkinsfile/

// Command to run command inside a docker container

pipeline {
    // Each stage specify its own agent
    agent any

    // Setup common job properties
    options {
        ansiColor('xterm')
        timestamps()
        timeout(time: 120, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    // Build stages
    stages {
        stage('Get sources') {
            steps {
                checkoutSrcs()
                stash name: 'srcs', excludes: '.git/'
                milestone label: 'Sources ready', ordinal: 1
            }
        }
        stage('Build & Test') {
            steps {
              sh 'cd jvm-packages; mvn clean compile package'
            }
        }
        stage("Publish Release from master") {
            when {
                expression {
                    env.GIT_BRANCH == 'origin/master'
                }
            }
            steps {
                sh 'cd jvm-packages; mvn -DaltDeploymentRepository=apixio.releases.build::default::https://repos.apixio.com/artifactory/releases/ deploy'
            }
        }
    }
}

// initialize source codes
def checkoutSrcs() {
  retry(5) {
    try {
      timeout(time: 2, unit: 'MINUTES') {
        checkout scm
        sh 'git submodule update --init'
      }
    } catch (exc) {
      deleteDir()
      error "Failed to fetch source codes"
    }
  }
}

/**
 * Creates cmake and make builds
 */
def buildFactory(buildName, conf) {
    def os = conf["os"]
    def nodeReq = conf["withGpu"] ? "${os} && gpu" : "${os}"
    def dockerTarget = conf["withGpu"] ? "gpu" : "cpu"
    [ ("cmake_${buildName}") : { buildPlatformCmake("cmake_${buildName}", conf, nodeReq, dockerTarget) },
      ("make_${buildName}") : { buildPlatformMake("make_${buildName}", conf, nodeReq, dockerTarget) }
    ]
}

/**
 * Build platform and test it via cmake.
 */
def buildPlatformCmake(buildName, conf, nodeReq, dockerTarget) {
    def opts = cmakeOptions(conf)
    // Destination dir for artifacts
    def distDir = "dist/${buildName}"
    // Build node - this is returned result
    node(nodeReq) {
        unstash name: 'srcs'
        echo """
        |===== XGBoost CMake build =====
        |  dockerTarget: ${dockerTarget}
        |  cmakeOpts   : ${opts}
        |=========================
        """.stripMargin('|')
        // Invoke command inside docker
        sh """
        ${dockerRun} ${dockerTarget} tests/ci_build/build_via_cmake.sh ${opts}
        ${dockerRun} ${dockerTarget} tests/ci_build/test_${dockerTarget}.sh
        ${dockerRun} ${dockerTarget} bash -c "cd python-package; python setup.py bdist_wheel"
        rm -rf "${distDir}"; mkdir -p "${distDir}/py"
        cp xgboost "${distDir}"
        cp -r lib "${distDir}"
        cp -r python-package/dist "${distDir}/py"
        """
        archiveArtifacts artifacts: "${distDir}/**/*.*", allowEmptyArchive: true
    }
}

/**
 * Build platform via make
 */
def buildPlatformMake(buildName, conf, nodeReq, dockerTarget) {
    def opts = makeOptions(conf)
    // Destination dir for artifacts
    def distDir = "dist/${buildName}"
    // Build node
    node(nodeReq) {
        unstash name: 'srcs'
        echo """
        |===== XGBoost Make build =====
        |  dockerTarget: ${dockerTarget}
        |  makeOpts    : ${opts}
        |=========================
        """.stripMargin('|')
        // Invoke command inside docker
        sh """
        ${dockerRun} ${dockerTarget} tests/ci_build/build_via_make.sh ${opts}
        """
    }
}

def makeOptions(conf) {
    return ([
        conf["withGpu"] ? 'PLUGIN_UPDATER_GPU=ON' : 'PLUGIN_UPDATER_GPU=OFF',
        conf["withOmp"] ? 'USE_OPENMP=1' : 'USE_OPENMP=0']
        ).join(" ")
}


def cmakeOptions(conf) {
    return ([
        conf["withGpu"] ? '-DPLUGIN_UPDATER_GPU:BOOL=ON' : '',
        conf["withOmp"] ? '-DOPEN_MP:BOOL=ON' : '']
        ).join(" ")
}

def getBuildName(conf) {
    def gpuLabel = conf['withGpu'] ? "_gpu" : "_cpu"
    def ompLabel = conf['withOmp'] ? "_omp" : ""
    def pyLabel = "_py${conf['pythonVersion']}"
    return "${conf['os']}${gpuLabel}${ompLabel}${pyLabel}"
}

