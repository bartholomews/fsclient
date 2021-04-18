package io.bartholomews.scalatestudo

import io.bartholomews.scalatestudo.matchers.StubbedIO
import io.bartholomews.scalatestudo.wiremock.WiremockServer
import org.scalatest.Suite

trait TestudoSuite extends Suite with WiremockServer with StubbedIO
