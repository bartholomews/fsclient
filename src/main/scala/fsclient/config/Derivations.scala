package fsclient.config

import pureconfig.{ConfigReader, Derivation}

object Derivations {

  private def customKeyConfigReader[A](key: String)(implicit reader: ConfigReader[A]): ConfigReader[A] =
    ConfigReader.fromCursor { cur =>
      for {
        objCur <- cur.asObjectCursor
        atKeyObjCursor <- objCur.atKey(key)
        configObjCursor <- atKeyObjCursor.asObjectCursor
        res <- implicitly[ConfigReader[A]].from(configObjCursor)
      } yield res
    }

  def withCustomKey[A](key: String)(implicit reader: ConfigReader[A]): Derivation[ConfigReader[A]] =
    Derivation.Successful(customKeyConfigReader(key))
}
