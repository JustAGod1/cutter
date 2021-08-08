package ru.justagod.mincer.pipeline

class RawChainSegment(val pipeline: MincerPipeline<*, *>, val next: RawChainSegment?) {
}