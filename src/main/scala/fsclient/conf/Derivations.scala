package fsclient.conf

import pureconfig.{ConfigReader, Derivation}

private[fsclient] object Derivations {

  private def customKeyConfigReader[A](key: String)(implicit reader: ConfigReader[A]): ConfigReader[A] =
    ConfigReader.fromCursor { cur =>
      for {
        objCur <- cur.asObjectCursor
        appNameObjCursor <- objCur.atKey(key)
        appConfigObjCursor <- appNameObjCursor.asObjectCursor
        res <- implicitly[ConfigReader[A]].from(appConfigObjCursor)
      } yield res
    }

  def withCustomKey[A](key: String)(implicit reader: ConfigReader[A]): Derivation[ConfigReader[A]] =
    Derivation.Successful(customKeyConfigReader(key))
}
