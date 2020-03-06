**Что это такое**

Вырезалка это плагин для градла. Она нужна для автоматического вырезания клиентского кода из серверных билдов.

Разграничение серверного кода и клиентского происходит через аннотации.

Относительно предыдущей версии тут добавились только автотесты и одна классная фича 👀


**Установка**

Тут все стало значительно проще. Я все-таки обзавелся доменом в мавен централе и теперь применение снапшота выглядит вот так:


```
buildscript {

    repositories {

        maven {

            url 'https://oss.sonatype.org/content/repositories/snapshots/'

        }

    }

    dependencies {

        classpath 'tech.justagod:cutter:2.0.1-SNAPSHOT'

    }

}

apply plugin: 'cutter'
```

Stable версии уже стоит смотреть в поиске.



Пример для 1.7.10


```
buildscript {

    repositories {

        mavenCentral()

        maven {

            name = "forge"

            url = "https://files.minecraftforge.net/maven"

        }

        maven {

            name = "maven"

            url = "https://repo1.maven.org/maven2"

        }

        maven {

            name = "sonatype"

            url = "https://oss.sonatype.org/content/repositories/snapshots/"

        }

    }

    dependencies {

        classpath 'tech.justagod:cutter:2.0.1-SNAPSHOT'

        classpath 'net.minecraftforge.gradle:ForgeGradle:1.2-SNAPSHOT'

    }

}



apply plugin: 'cutter'

apply plugin: 'forge'
```


И для 1.8


```
buildscript {

    repositories {

        mavenCentral()

        maven {

            name = "sonatype"

            url = "https://oss.sonatype.org/content/repositories/snapshots/"

        }

    }

    dependencies {

        classpath 'tech.justagod:cutter:2.0.1-SNAPSHOT'

    }

}

plugins {

    id "net.minecraftforge.gradle.forge" version "2.0.2"

}

apply plugin: 'cutter'
```


**Настройка**

Давайте сразу обозначим, что я буду называть все конструкции в жаве сущностями.



Тут все осталось практически без изменений. Я только убрал лишние параметры.

Самая простая настройка все еще выглядит вот так:

```
cutter {

    annotation = "anno.SideOnly"

    def serverSide = side 'server'

    def clientSide = side 'client'

    builds {

        client {

            targetSides = [clientSide]

            primalSides = [clientSide, serverSide]

        }

        server {

            targetSides = [serverSide]

            primalSides = [clientSide, serverSide]

        }

    }

}
```

`annotation` - полное имя аннотации, которой вы будете размечать стороны в коде.



Строчки с `serverSide` и `clientSide` просто объявляют 2 переменные которые хранят в себе 2 дескриптора сторон для моего энума.

```public enum  Side { CLIENT, SERVER }```

который стоит в моей аннотации


```
@Retention(RetentionPolicy.RUNTIME)

public @interface SideOnly {



    Side[] value();

}

```



Блок `builds` хранит в себе варианты собираемых жарников. Я объявил всего 2 варианта: client и server. По этому поводу вырезалка сделает 3 таска `buildClient`, `buildServer` и `buildAll`

В каждом из вариантов есть 2 поля `targetSides` и `primalSides`.

С `targetSides` все предельно ясно. Только сущности, существующие в одной из `targetSides` остануться в живых после обработки.

С `primalSides` же всегда возникают непонятки. `primalSides` - это стороны которые присвоены абсолютно каждому объекту в проекте до тех пор пока вы не скажете обратное.





Во время анализа вырезалка будет назначать сущностям стороны, просто иттерируясь по значениям `value` в аннотации. На этом этапе никто не смотрит на дескрипторы, объявленные в конфиге.

После того как будет построено дерево проекта начнется вырезание: стороны каждой сущности сравнят с `targetSides`, и, если ни одна сторона сущности не содержится в `targetSides`, сущность будет вырезана.  На этом этапе уже никто не смотрит на аннотации.

Это можно абузить👀



**Немного про анализ**

Вырезалка будет ожидать, что вы будете ставить аннотацию над классом, методом, полем или в package-info. Помимо очевидных эффектов (вырезание конкретно данной сущности) еще происходят следующие явления:

Все наследники удаленного класса/интерфейса также будут вырезаны
Все методы, переопределяющие удаленный метод, также будут удалены
Все классы объявленные в удаленном классе или методе также будут удалены
Методы, имплементящие тело лямбд в удаленных методах, также будут удалены
В остальном вырезалка ведет себе достаточно интуитивно👀



**Инвокаторы**

Начнем с того, что это рабочее название.

Я считаю, что это тотальная киллер фича относительно предыдущей версии вырезалки.

Мне достаточно часто жаловались на вот такой кейс

```
class SomeClass {

    @SideOnly(SERVER)

    public String someField = "someValue";

  

    public void methodThatExistsOnBothSides() {

      

    }

}


// Crash here: FieldNotFoundError: someField

new SomeClass().methodThatExistsOnBothSides();
```



Так происходило из-за того, что вырезалка удаляла метод, но не удаляла ее инициализацию из конструктора.

По этому поводу я вот решил стырить еще одно решение, и предлагаю следующий конструкт:

```
class SomeClass {

    @SideOnly(SERVER)

    public String someField;

  

    public SomeClass() {

        Invoke.server(() -> someField = "someValue");

    }



    public void methodThatExistsOnBothSides() {

      

    }

}



// Crash won't happen

new SomeClass().methodThatExistsOnBothSides();
```



Суть в том, что вырезалка вырежет все внутри лямбды не тронув остальное. Таким образом метод будет инициализирован, но только на сервере.

В целом эффект похож на if по константе, но в будущем будет понятно почему это плохой подход👀



Чтобы эта штука заработала нужно немного изменить конфиг, на что-то типа


```
cutter {

    annotation = "anno.SideOnly"

    def serverSide = side('server')

    def clientSide = side('client')

    invocation {

        name = 'test9.ServerInvoke'

        sides = [serverSide]

        method = 'run()V'

    }

    invocation {

        name = 'test9.ClientInvoke'

        sides = [clientSide]

        method = 'run()V'

    }

    builds {

        client {

            targetSides = [clientSide]

            primalSides = [clientSide, serverSide]

        }

        server {

            targetSides = [serverSide]

            primalSides = [clientSide, serverSide]

        }

    }

}
```

Обратите внимание на блоки `invocation`. Здесь важный момент в том, что они должны идти до блока `builds`.

Разберем поля



`name` - полное имя класса
`sides` - стороны на которых содержимое вырезано не будет
`method` - сигнатура метода из которого нужно все вырезать. Он может принимать сколь угодно много параметров и возвращать, что угодно. Тут ограничений не накладывается. Единственное что после вырезания он будет возвращать `null`, в случае когда он возвращает объект, или дефолтное значение, в случае когда он возвращает примитив или обертку над примитивом.


Вообще в качестве инвокатора может выступать абсолютно любой класс. Вырезалка просто будет искать наследников или `invokedynamic` и вырезать им внутренности. Лично я советую в качестве инвокаторов все же использовать ф-ые интерфейсы.



**Заключение**

Собственно, если после прочтения этой темы что-то осталось не ясно, можно попробовать почитать старую тему вот тут. 



Примеры использования думаю можно достать из моих автотестов вот тут или тут. Надеюсь в ближайшее время сделать нормальные экзамплы в репо.



Доставать плагин я предлагаю все еще со снапшотов, но как только вот тут появится что-то новенькое я сообщу.



А и да, в прошлой версии была такая классная штука как вывод дерева проекта при сборке. Чтобы ее включить, теперь нужно выставить `-Dprint-sides=true` в `gradle.properties`.