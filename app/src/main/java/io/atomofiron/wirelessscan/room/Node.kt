package io.atomofiron.wirelessscan.room

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.content.res.Resources
import android.graphics.Color
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Parcel
import android.os.Parcelable
import io.atomofiron.wirelessscan.R

@Entity(tableName = "nodes", primaryKeys = arrayOf("bssid"))
class Node : Parcelable {
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(level)
        dest.writeInt(frequency)
        dest.writeString(capabilities)
        dest.writeString(essid)
        dest.writeString(bssid)
        dest.writeInt(ch)
        dest.writeString(manufacturer)
    }

    override fun describeContents(): Int = 0

    var level = 0
        set(value) { field = value; pwColor = getPowerColor(value) }
    var frequency = 0
        set(value) { field = value; parseFrequency(value) }
    var capabilities = ""
        set(value) { field = value; parseCapabilities(value) }
    var essid = ""
        set(value) { field = value; essidColor = if (value.isEmpty()) yellow else grey }
    var bssid = ""

    var ch = 0
    @Ignore
    lateinit var enc: String
        private set
    @Ignore
    lateinit var chi: String
        private set
    @Ignore
    lateinit var wps: String
        private set
    var manufacturer = ""

    @Ignore
    var pwColor = 0
        private set
    @Ignore
    var chColor = 0
        private set
    @Ignore
    var encColor = 0
        private set
    @Ignore
    var chiColor = 0
        private set
    @Ignore
    var wpsColor = 0
        private set
    @Ignore
    var essidColor = 0
        private set
    @Ignore
    val bssidColor = grey

    internal constructor()

    constructor(sr: ScanResult) : this() {
        level = sr.level
        frequency = sr.frequency
        ch = getChanel(frequency)
        capabilities = sr.capabilities
        essid = sr.SSID
        bssid = sr.BSSID
    }

    private fun is5G(): Boolean = frequency >= 4915

    private fun parseFrequency(frequency: Int) {
        ch = getChanel(frequency)
        chColor = if (is5G()) blue_light else grey
    }

    private fun parseCapabilities(capabilities: String) {
        specifyEnc(capabilities)
        specifyChi(capabilities)
        specifyWps(capabilities)
    }

    private fun specifyEnc(capabilities: String) {
        enc = "OPN"
        encColor = green
        if (capabilities.contains("WPA")) {
            enc = if (capabilities.contains("WPA2")) "WPA2" else "WPA"
            encColor = yellow_middle
        } else if (capabilities.contains("WEP")) {
            enc = "WEP"
            encColor = sky_light
        }
        if (capabilities.contains("EAP"))
            encColor = red_light
    }

    private fun specifyChi(capabilities: String) {
        chi = if (capabilities.contains("CCMP")) "CCMP" else ""
        chiColor = grey

        if (capabilities.contains("TKIP")) {
            chi = if (chi.isEmpty()) "  TKIP" else "+TKIP"
            chiColor = if (capabilities.contains("preauth")) sky else sky_white
        }
    }

    private fun specifyWps(capabilities: String) {
        val yes = capabilities.contains("WPS")

        wps = if (yes) "yes" else "no"
        wpsColor = if (yes) green_high else red_high
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other::class.java != Node::class.java)
            return false

        val o = other as Node
        return o.essid == essid && o.bssid == bssid
    }

    fun compare(bssid: String, essid: String, hidden: Boolean): Boolean =
            this.bssid == bssid && (this.essid == essid || this.level > MIN_LEVEL && this.essid.isEmpty() && hidden)

    fun compare(node: Node, smart: Boolean): Boolean = this.essid == node.essid &&
            if (smart)
                this.bssid.startsWith(node.bssid.substring(0, 8))
            else
                this.bssid == node.bssid

    fun getNotEmptyESSID(): String = if (essid.isEmpty()) bssid else essid

/*    override fun describeContents(): Int {
        return 0
    }
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(level)
        dest.writeInt(frequency)
        dest.writeString(capabilities)
        dest.writeString(essid)
        dest.writeString(bssid)
    }*/

    companion object {
        val MIN_LEVEL = -100 // WifiManager.MIN_LEVEL

        var transparent = 0
            private set
        var black_lite = 0
            private set
        var red_lite= 0
            private set

        private var red_middle = 0
        private var grey = 0
        private var blue_light = 0
        private var green = 0
        private var yellow_middle = 0
        private var sky_light = 0
        private var red_light = 0
        private var sky = 0
        private var sky_white = 0
        private var green_high = 0
        private var red_high = 0
        private var yellow = 0
        var green_light = 0; private set

        fun initColors(resources: Resources) {
            transparent = resources.getColor(R.color.transparent)
            black_lite = resources.getColor(R.color.black_lite)
            red_lite = resources.getColor(R.color.red_lite)
            red_middle = resources.getColor(R.color.red_middle)
            grey = resources.getColor(R.color.grey)
            blue_light = resources.getColor(R.color.blue_light)
            green = resources.getColor(R.color.green)
            yellow_middle = resources.getColor(R.color.yellow_middle)
            sky_light = resources.getColor(R.color.sky_light)
            red_light = resources.getColor(R.color.red_light)
            sky = resources.getColor(R.color.sky)
            sky_white = resources.getColor(R.color.sky_white)
            green_high = resources.getColor(R.color.green_high)
            red_high = resources.getColor(R.color.red_high)
            yellow = resources.getColor(R.color.yellow)
            green_light = resources.getColor(R.color.green_light)
        }

        private fun getPowerColor(level: Int): Int {
            val pwr = WifiManager.calculateSignalLevel(level, 512)
            var red = if (pwr < 256) "ff" else Integer.toHexString(512 - pwr)
            var green = if (pwr >= 256) "ff" else Integer.toHexString(pwr)

            if (red.length < 2)
                red = "0" + red

            if (green.length < 2)
                green = "0" + green

            return Color.parseColor("#ff$red${green}00")
        }

        private fun getChanel(frequency: Int): Int {
            var fr = frequency
            var ans = 0
            if (fr in 2412..2484) {
                if (fr == 2484) return 14
                while (fr >= 2412) {
                    fr -= 5
                    ans++
                }
            } else if (fr in 3658..3692) {
                ans = 130
                while (fr >= 3655) {
                    fr -= 5
                    ans++
                }
            } else if (fr in 4940..4990
                    && fr % 5 != 0) {
                ans = 19
                while (fr >= 4940) {
                    fr -= 7
                    ans++
                }
            } else if (fr in 4915..4980) {
                ans = 182
                while (fr >= 4915) {
                    fr -= 5
                    ans++
                }
            } else if (fr in 5035..5825) {
                ans = 6
                while (fr >= 5035) {
                    fr -= 5
                    ans++
                }
            }
            return ans
        }

        fun parseScanResults(list: List<ScanResult>) : ArrayList<Node> {
            val nodes = ArrayList<Node>()
            list.forEach { it -> nodes.add(Node(it)) }

            return nodes
        }
    }

/*    private val CREATOR: Parcelable.Creator<Node> = object : Parcelable.Creator<Node> {
        override fun createFromParcel(parcel: Parcel): Node {
            val node = Node()
            node.level = parcel.readInt()
            node.frequency = parcel.readInt()
            node.capabilities = parcel.readString()
            node.essid = parcel.readString()
            node.bssid = parcel.readString()

            return node
        }

        override fun newArray(size: Int): Array<Node?> {
            return arrayOfNulls(size)
        }
    }*/
}