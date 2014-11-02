package com.thetestpeople.trt.model.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Verify the MockDao behaves correctly (so that we can happily use it in higher-level tests)
 */
@RunWith(classOf[JUnitRunner])
class MockDaoTest extends DaoTest {

  def createDao = new MockDao

}