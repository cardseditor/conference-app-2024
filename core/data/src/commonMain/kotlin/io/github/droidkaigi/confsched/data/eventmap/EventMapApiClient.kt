package io.github.droidkaigi.confsched.data.eventmap

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.http.GET
import io.github.droidkaigi.confsched.data.NetworkService
import io.github.droidkaigi.confsched.data.eventmap.response.EventMapResponse
import io.github.droidkaigi.confsched.data.eventmap.response.MessageResponse
import io.github.droidkaigi.confsched.model.EventMapEvent
import io.github.droidkaigi.confsched.model.MultiLangText
import io.github.droidkaigi.confsched.model.RoomIcon
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal interface EventMapApi {
    @GET("/events/droidkaigi2024/eventmap")
    suspend fun getEventMap(): EventMapResponse
}

public class DefaultEventMapApiClient(
    private val networkService: NetworkService,
    ktorfit: Ktorfit,
) : EventMapApiClient {

    private val eventMapApi = ktorfit.create<EventMapApi>()

    public override suspend fun eventMapEvents(): PersistentList<EventMapEvent> {
        return networkService {
            eventMapApi.getEventMap()
        }.toEventMapList()
    }
}

public interface EventMapApiClient {

    public suspend fun eventMapEvents(): PersistentList<EventMapEvent>
}

public fun EventMapResponse.toEventMapList(): PersistentList<EventMapEvent> {
    val roomIdToNameMap = this.rooms.associateBy({ it.id }, { it.name.ja to it.name.en })

    return this.events
        .mapNotNull { event ->
            roomIdToNameMap[event.roomId]?.let { roomName ->
                EventMapEvent(
                    name = MultiLangText(
                        jaTitle = event.title.ja,
                        enTitle = event.title.en,
                    ),
                    roomName = MultiLangText(
                        jaTitle = roomName.first,
                        enTitle = roomName.second,
                    ),
                    roomIcon = roomName.second.toRoomIcon(),
                    description = MultiLangText(
                        jaTitle = event.i18nDesc.ja,
                        enTitle = event.i18nDesc.en,
                    ),
                    moreDetailsUrl = event.moreDetailsUrl,
                    message = event.message.toMultiLangText(),
                )
            }
        }
        .toPersistentList()
}

private fun String.toRoomIcon(): RoomIcon = when (this) {
    "Iguana" -> RoomIcon.Square
    "Hedgehog" -> RoomIcon.Diamond
    "Giraffe" -> RoomIcon.Circle
    "Flamingo" -> RoomIcon.Rhombus
    "Jellyfish" -> RoomIcon.Triangle
    else -> RoomIcon.None
}

private fun MessageResponse.toMultiLangText() =
    if (ja != null && en != null) MultiLangText(jaTitle = ja, enTitle = en) else null
