package gg.rsmod.game.message.codec.encoder

import gg.rsmod.game.message.MessageStructure
import gg.rsmod.game.message.codec.MessageEncoder
import gg.rsmod.game.message.impl.SetBigVarpMessage
import gg.rsmod.game.message.impl.SetSmallVarpMessage

/**
 * Responsible for extracting values from the [SetSmallVarpMessage] based on keys.
 *
 * @author Tom <rspsmods@gmail.com>
 */
class BigVarpEncoder(override val structure: MessageStructure) : MessageEncoder<SetBigVarpMessage>(structure) {

    override fun extract(message: SetBigVarpMessage, key: String): Number = when (key) {
        "id" -> message.id
        "value" -> message.value
        else -> throw Exception("Unhandled value key.")
    }

    override fun extractBytes(message: SetBigVarpMessage, key: String): ByteArray = throw Exception("Unhandled value key.")

}