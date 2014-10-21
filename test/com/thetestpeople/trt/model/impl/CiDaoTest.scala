package com.thetestpeople.trt.model.impl

import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.mother.{ TestDataFactory ⇒ F }
import com.thetestpeople.trt.utils.UriUtils._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.joda.time.DateTime
import org.joda.time.Duration
import java.net.URI

trait CiDaoTest { self: AbstractDaoTest ⇒

  "Inserting and retrieving a new import spec" should "persist all the data" in transaction { dao ⇒
    val specId = dao.newCiImportSpec(F.ciImportSpec(
      ciType = CiType.TeamCity,
      jobUrl = DummyData.JobUrl,
      pollingInterval = DummyData.PollingInterval,
      importConsoleLog = true,
      lastCheckedOpt = Some(DummyData.LastChecked)))

    val Some(spec) = dao.getCiImportSpec(specId)
    spec.ciType should equal(CiType.TeamCity)
    spec.jobUrl should equal(DummyData.JobUrl)
    spec.pollingInterval should equal(DummyData.PollingInterval)
    spec.importConsoleLog should equal(true)
    spec.lastCheckedOpt should equal(Some(DummyData.LastChecked))
  }

  "Deleting animport spec" should "delete it if present" in transaction { dao ⇒
    val specId = dao.newCiImportSpec(F.ciImportSpec())

    val success = dao.deleteCiImportSpec(specId)

    success should be(true)
    dao.getCiImportSpec(specId) should be(None)
    dao.deleteCiImportSpec(specId) should be(false)
  }

  "Deleting an import spec" should "indicate if it wasnt present to start with" in transaction { dao ⇒
    dao.deleteCiImportSpec(Id.dummy) should be(false)
  }

  "Updating last checked date of an import spec" should "persist the change" in transaction { dao ⇒
    val specId = dao.newCiImportSpec(F.ciImportSpec(lastCheckedOpt = None))

    val success = dao.updateCiImportSpec(specId, lastCheckedOpt = Some(DummyData.LastChecked))

    success should be(true)
    val Some(updatedSpec) = dao.getCiImportSpec(specId)
    updatedSpec.lastCheckedOpt should be(Some(DummyData.LastChecked))
  }

  "Updating various fields of an import spec" should "persist the changes" in transaction { dao ⇒
    val specId = dao.newCiImportSpec(F.ciImportSpec(
      jobUrl = uri("http://www.example.com"),
      pollingInterval = 5.minutes,
      importConsoleLog = true))

    val success = dao.updateCiImportSpec(F.ciImportSpec(
      jobUrl = uri("http://www.elsewhere.com"),
      pollingInterval = 10.minutes,
      importConsoleLog = false).copy(id = specId))

    success should be(true)
    val Some(updatedSpec) = dao.getCiImportSpec(specId)
    updatedSpec.jobUrl should be(uri("http://www.elsewhere.com"))
    updatedSpec.pollingInterval should be(10.minutes: Duration)
    updatedSpec.importConsoleLog should be(false)
  }

  "Adding a CI build" should "persist all the data" in transaction { dao ⇒
    val batchId = dao.newBatch(F.batch())
    val jobId = dao.ensureCiJob(F.ciJob())
    val specId = dao.newCiImportSpec(F.ciImportSpec())
    val build = CiBuild(
      jobId = jobId,
      batchId = batchId,
      buildNumberOpt = Some(DummyData.BuildNumber),
      buildNameOpt = Some(DummyData.BatchName),
      importTime = DummyData.ImportTime,
      buildUrl = DummyData.BuildUrl,
      importSpecIdOpt = Some(specId))
    dao.newCiBuild(build)

    val Some(buildAgain) = dao.getCiBuild(DummyData.BuildUrl)
    buildAgain should equal(build)
  }

  "Fetching builds associated with a job URL" should "return the builds" in transaction { dao ⇒
    val batchId1 = dao.newBatch(F.batch())
    val batchId2 = dao.newBatch(F.batch())
    val jobId = dao.ensureCiJob(F.ciJob(url = DummyData.JobUrl))
    val specId = dao.newCiImportSpec(F.ciImportSpec())
    val build1 = F.jenkinsBuild(jobId = jobId, batchId = batchId1, buildUrl = DummyData.BuildUrl, importSpecIdOpt = Some(specId))
    val build2 = F.jenkinsBuild(jobId = jobId, batchId = batchId2, buildUrl = DummyData.BuildUrl2, importSpecIdOpt = Some(specId))
    dao.newCiBuild(build1)
    dao.newCiBuild(build2)

    val builds = dao.getCiBuilds(specId)

    builds should contain theSameElementsAs (Seq(build1, build2))
  }

  "The DAO" should "return all the build URLs" in transaction { dao ⇒
    def addBuild(buildUrl: URI) {
      val batchId = dao.newBatch(F.batch())
      val jobId = dao.ensureCiJob(F.ciJob())
      dao.newCiBuild(F.jenkinsBuild(batchId, jobId = jobId, buildUrl = buildUrl))
    }
    val buildUrls = List(
      "http://www.example.com/1",
      "http://www.example.com/2",
      "http://www.example.com/3").map(uri)
    buildUrls.foreach(addBuild)

    dao.getCiBuildUrls should contain theSameElementsAs (buildUrls)
  }

  "Adding a new Jenkins job" should "persist all job data" in transaction { dao ⇒
    val jobId = dao.ensureCiJob(F.ciJob(
      url = DummyData.JobUrl,
      name = DummyData.JobName))

    val Seq(job) = dao.getCiJobs()
    job.id should equal(jobId)
    job.url should equal(DummyData.JobUrl)
    job.name should equal(DummyData.JobName)
  }

  "Ensuring a new job exists" should "have no effect if it already exists" in transaction { dao ⇒
    val jobId = dao.ensureCiJob(F.ciJob(url = DummyData.JobUrl))
    val jobIdAgain = dao.ensureCiJob(F.ciJob(url = DummyData.JobUrl))
    jobIdAgain should equal(jobId)
    val Seq(job) = dao.getCiJobs()
    job.id should equal(jobId)
  }

  "Inserting and retrieving configuration" should "persist all the data" in transaction { dao ⇒
    val params: List[JenkinsJobParam] = List(JenkinsJobParam(
      param = DummyData.ParamName,
      value = DummyData.ParamValue))
    val config = JenkinsConfiguration(
      usernameOpt = Some(DummyData.Username),
      apiTokenOpt = Some(DummyData.ApiToken),
      rerunJobUrlOpt = Some(DummyData.JobUrl),
      authenticationTokenOpt = Some(DummyData.AuthenticationToken))
    val fullConfig = FullJenkinsConfiguration(config, params)

    dao.updateJenkinsConfiguration(fullConfig)

    val fullConfigAgain = dao.getJenkinsConfiguration
    fullConfigAgain should equal(fullConfig)
  }

}