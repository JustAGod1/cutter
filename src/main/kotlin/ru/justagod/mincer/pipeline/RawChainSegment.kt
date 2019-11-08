package ru.justagod.mincer.pipeline

class RawChainSegment(val pipeline: Pipeline<*, *>, val next: RawChainSegment?) {
}