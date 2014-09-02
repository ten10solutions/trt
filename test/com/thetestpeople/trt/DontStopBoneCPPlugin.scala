package com.thetestpeople.trt

import play.api.db.BoneCPPlugin
import play.api.Application

/**
 * For tests, prevent stopping the BoneCP plug-in to avoid "Attempting to obtain a connection from a pool that has
 *  already been shutdown" exceptions.
 */
class DontStopBoneCPPlugin(app: Application) extends BoneCPPlugin(app) {

  override def onStop() { /* super.onStop() */ }

}

