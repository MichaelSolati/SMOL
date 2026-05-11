package com.michaelsolati.smol.di

import com.michaelsolati.smol.service.AudioCompressor
import com.michaelsolati.smol.service.AudioCompressorImpl
import com.michaelsolati.smol.service.ImageCompressor
import com.michaelsolati.smol.service.ImageCompressorImpl
import com.michaelsolati.smol.service.VideoCompressor
import com.michaelsolati.smol.service.VideoCompressorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CompressionModule {

    @Binds
    @Singleton
    abstract fun bindImageCompressor(impl: ImageCompressorImpl): ImageCompressor

    @Binds
    @Singleton
    abstract fun bindVideoCompressor(impl: VideoCompressorImpl): VideoCompressor

    @Binds
    @Singleton
    abstract fun bindAudioCompressor(impl: AudioCompressorImpl): AudioCompressor
}
