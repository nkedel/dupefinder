package us.n8l.duplicatefinder

import us.n8l.duplicatefinder.digests.MD5
import us.n8l.duplicatefinder.digests.SHA256
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.*
import java.util.concurrent.atomic.AtomicReference


