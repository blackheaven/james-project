/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 *****************************************************************/

package org.apache.james.server.blob.deduplication

import java.io.InputStream

import com.google.common.base.Preconditions
import com.google.common.hash.{Hashing, HashingInputStream}
import com.google.common.io.{ByteSource, FileBackedOutputStream}
import javax.inject.{Inject, Named}
import org.apache.commons.io.IOUtils
import org.apache.james.blob.api.{BlobId, BlobStore, BucketName, DumbBlobStore}
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.core.scala.publisher.SMono
import reactor.util.function.{Tuple2, Tuples}

object DeDuplicationBlobStore {
  val DEFAULT_BUCKET = "defaultBucket"
  val LAZY_RESOURCE_CLEANUP = false
  val FILE_THRESHOLD = 10000
}

class DeDuplicationBlobStore @Inject()(dumbBlobStore: DumbBlobStore,
                                       @Named("defaultBucket") defaultBucketName: BucketName,
                                       blobIdFactory: BlobId.Factory) extends BlobStore {

  override def save(bucketName: BucketName, data: Array[Byte], storagePolicy: BlobStore.StoragePolicy): Publisher[BlobId] = {
    Preconditions.checkNotNull(bucketName)
    Preconditions.checkNotNull(data)

    val blobId = blobIdFactory.forPayload(data)

    SMono(dumbBlobStore.save(bucketName, blobId, data))
      .`then`(SMono.just(blobId))
  }

  override def save(bucketName: BucketName, data: InputStream, storagePolicy: BlobStore.StoragePolicy): Publisher[BlobId] = {
    Preconditions.checkNotNull(bucketName)
    Preconditions.checkNotNull(data)
    val hashingInputStream = new HashingInputStream(Hashing.sha256, data)
    val sourceSupplier: FileBackedOutputStream => SMono[BlobId] = (fileBackedOutputStream: FileBackedOutputStream) => saveAndGenerateBlobId(bucketName, hashingInputStream, fileBackedOutputStream)
    Mono.using(() => new FileBackedOutputStream(DeDuplicationBlobStore.FILE_THRESHOLD),
      sourceSupplier,
      (fileBackedOutputStream: FileBackedOutputStream) => fileBackedOutputStream.reset(),
      DeDuplicationBlobStore.LAZY_RESOURCE_CLEANUP)
  }

  private def saveAndGenerateBlobId(bucketName: BucketName, hashingInputStream: HashingInputStream, fileBackedOutputStream: FileBackedOutputStream): SMono[BlobId] =
    SMono.fromCallable(() => {
      IOUtils.copy(hashingInputStream, fileBackedOutputStream)
      Tuples.of(blobIdFactory.from(hashingInputStream.hash.toString), fileBackedOutputStream.asByteSource)
    })
      .flatMap((tuple: Tuple2[BlobId, ByteSource]) =>
        SMono(dumbBlobStore.save(bucketName, tuple.getT1, tuple.getT2))
          .`then`(SMono.just(tuple.getT1)))


  override def readBytes(bucketName: BucketName, blobId: BlobId): Publisher[Array[Byte]] = {
    Preconditions.checkNotNull(bucketName)

    dumbBlobStore.readBytes(bucketName, blobId)
  }

  override def read(bucketName: BucketName, blobId: BlobId): InputStream = {
    Preconditions.checkNotNull(bucketName)

    dumbBlobStore.read(bucketName, blobId)
  }

  override def getDefaultBucketName: BucketName = defaultBucketName

  override def deleteBucket(bucketName: BucketName): Publisher[Void] = {
    dumbBlobStore.deleteBucket(bucketName)
  }

  override def delete(bucketName: BucketName, blobId: BlobId): Publisher[Void] = {
    Preconditions.checkNotNull(bucketName)
    Preconditions.checkNotNull(blobId)

    dumbBlobStore.delete(bucketName, blobId)
  }
}