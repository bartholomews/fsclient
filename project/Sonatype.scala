object Sonatype extends sbt.Import {

  private val gpg = Credentials(
    "GnuPG Key ID",
    "gpg",
    sys.env("SONATYPE_GPG_PUBLIC"),
    "ignored" // this field is ignored; passwords are supplied by pinentry
  )

  private val sonatype = Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    sys.env("SONATYPE_OSS_USER"),
    sys.env("SONATYPE_OSS_PASSWORD")
  )

  val credentials = List(gpg, sonatype)
}
