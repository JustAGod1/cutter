package ru.justagod.plugin.test.base.runner.trivial

class TrivialTestBuilder(private val name: String) {

    private lateinit var model: ImageModel
    private lateinit var src: String
    private val confNames = hashSetOf<String>()

    fun model(block: ImageModel.Directory.() -> Unit): TrivialTestBuilder {
        model = ImageModel.make(block)
        return this
    }

    fun src(value: String): TrivialTestBuilder {
        src = value

        return this
    }

    fun conf(vararg confName: String): TrivialTestBuilder {
        confNames += confName
        return this
    }

    fun build(): TrivialTestData {
        if (confNames.isEmpty()) error("Define at least one config")
        return TrivialTestData(name, src, confNames, model)
    }

}