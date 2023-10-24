import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class SBSServer {

    private val clients = mutableListOf<Socket>()

    fun start() {
        val serverSocket = ServerSocket(30003)
        Thread {
            while (true) {
                clients.add(serverSocket.accept())
            }
        }.start()
    }

    fun sendData(data: ADSBData) {
        Thread {
            val iterator = clients.iterator()
            while (iterator.hasNext()) {
                val socket = iterator.next()
                try {
                    val outputStream = socket.getOutputStream()
                    val writer = PrintWriter(outputStream)
                    data.ac.forEach { ac ->
                        if (ac.alt_baro.toIntOrNull() != null) {
                            writer.println(
                                createMessage(
                                    hex = ac.hex,
                                    flightId = ac.flight,
                                    altitude = ac.getAltitude(),
                                    lat = ac.lat,
                                    lon = ac.lon,
                                    squawk = ac.squawk
                                )
                            )
                            writer.println(
                                createMessage(
                                    transmissionType = 4,
                                    hex = ac.hex,
                                    flightId = ac.flight,
                                    groundSpeed = ac.gs,
                                    track = ac.track,
                                    squawk = ac.squawk
                                )
                            )
                            writer.flush()
                        }
                    }
                } catch (e: Exception) {
                    // we don't care
                }
            }
        }.start()
    }

    /**
     * DOCS: http://woodair.net/SBS/Article/Barebones42_Socket_Data.htm
     * Example: MSG,3,5,211,4CA2D6,10057,2008/11/28,14:53:50.594,2008/11/28,14:58:51.153,,37000,,,51.45735,-1.02826,,,0,0,0,0
     */
    private fun createMessage(
        messageType: String = "MSG",
        transmissionType: Int = 3,
        sessionId: Int = 5,
        aircraftId: Int = 0,
        hex: String = "",
        flightId: String = "",
        altitude: Int = 0,
        groundSpeed: Double = 0.0,
        track: Double = 0.0,
        lat: Double = 0.0,
        lon: Double = 0.0,
        squawk: String = ""
    ): String {
        val builder = StringBuilder()
        // The below basic data fields are standard for all messages (Field 2 used only for MSG)
        builder.append("$messageType,")
        builder.append("$transmissionType,")
        builder.append("$sessionId,")
        builder.append("$aircraftId,")
        builder.append("$hex,")
        builder.append("$flightId,")
        builder.append("${getDate()},")
        builder.append("${getTime()},")
        builder.append("${getDate()},")
        builder.append("${getTime()},")
        // The fields below contain specific aircraft information
        builder.append("${flightId},")
        builder.append("${altitude},")
        builder.append("${groundSpeed},")
        builder.append("${track},")
        builder.append("${formatCoordinate(lat)},")
        builder.append("${formatCoordinate(lon)},")
        builder.append(",") // vertical rate
        builder.append("${squawk},")
        builder.append(",") // alert
        builder.append(",") // emergency
        builder.append(",") // SPI
        builder.append("0") // isOnGround

        val message = builder.toString()
        println(message)
        return message
    }

    private fun getDate() = SimpleDateFormat("yyyy/MM/dd").format(Calendar.getInstance().time)

    private fun getTime() = SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().time)

    private fun formatCoordinate(coordinate: Double): String {
        return String.format("%.5f", coordinate)
    }
}