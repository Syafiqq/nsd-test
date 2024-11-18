package com.github.syafiqq.nsd_test_001.ui.dashboard

import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.syafiqq.nsd_test_001.databinding.FragmentDashboardBinding
import com.github.syafiqq.nsd_test_001.helper.setupWifiMulticast
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import javax.jmdns.ServiceTypeListener

private const val TAG_VC_MAIN_BROAD_REG = "VC-Main-Broad-Reg"
private const val TAG_VC_MAIN_DISC_REG = "VC-Main-Disc-Reg"
private const val TAG_VC = "VC-Dashboard-Frag"

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private var jmDNS: JmDNS? = null

    private var discoverLogHandler: ((String) -> Unit)? = null
    private var discoverServiceName: String? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d(TAG_VC, "onCreateView")

        val dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupMulticast()
        broadcastInitialService()

        return root
    }

    // connect to listener
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG_VC, "onViewCreated")

        binding.button1.setOnClickListener(this::onBroadcastingRegister)
        binding.button2.setOnClickListener(this::onBroadcastingUnregister)
        binding.button3.setOnClickListener(this::onDiscoveryRegister)
        binding.button4.setOnClickListener(this::onDiscoveryUnregister)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG_VC, "onDestroyView")

        _binding = null
    }

    override fun onStart() {
        super.onStart()

        Log.d(TAG_VC, "onStart")
    }

    override fun onResume() {
        super.onResume()

        Log.d(TAG_VC, "onResume")
    }

    override fun onPause() {
        super.onPause()

        Log.d(TAG_VC, "onPause")
    }

    override fun onStop() {
        super.onStop()

        Log.d(TAG_VC, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()

        destroyMulticast()
        Log.d(TAG_VC, "onDestroy")
    }

    fun onDiscoveryUnregister(view: View) {
        Log.d(TAG_VC, "onDiscoveryUnregister")
        discoverUnregisterService()
    }

    fun onDiscoveryRegister(view: View) {
        Log.d(TAG_VC, "onDiscoveryRegister")
        discoverRegisterService()
    }

    fun onBroadcastingUnregister(view: View) {
        Log.d(TAG_VC, "onBroadcastingUnregister")
        broadcastUnregisterService()
    }

    fun onBroadcastingRegister(view: View) {
        Log.d(TAG_VC, "onBroadcastingRegister")
        broadcastRegisterService(12345)
    }

    // Mark: - NSD Broadcast

    private fun broadcastInitialService() {
        val thread = Thread {
            try {
                val jmDNS = JmDNS.create(InetAddress.getLocalHost())
                this@DashboardFragment.jmDNS = jmDNS
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        thread.start()
    }

    private fun broadcastRegisterService(port: Int) {
        val serviceInfo = ServiceInfo.create("_nsdchat._tcp", "NsdChat", port, "")

        try {
            jmDNS?.registerService(serviceInfo)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG_VC_MAIN_BROAD_REG, "broadcastRegisterService", e)
        }
    }

    private fun broadcastUnregisterService() {
        try {
            jmDNS?.unregisterAllServices();
        } catch (e: IllegalArgumentException) {
            Log.e(TAG_VC_MAIN_BROAD_REG, "broadcastUnregisterService")
        }
    }

    // Mark: - NSD Discoveries

    private fun discoverRegisterService() {
        val serviceName =
            binding.radioGroup.findViewById<RadioButton>(binding.radioGroup.checkedRadioButtonId)

        val selectedService = if (serviceName === binding.customRadioButton) {
            binding.customServiceType?.text.toString()
        } else {
            serviceName?.text.toString()
        }

        if (selectedService == null) {
            binding.multiText1.setText("discoverRegisterService - Can't find selected service\n")
            return
        }

        binding.multiText1.setText("")
        discoverLogHandler = {
            requireActivity().runOnUiThread {
                binding.multiText1.setText("${binding.multiText1.text}\n$it\n")
            }
        }

        try {
            jmDNS?.addServiceListener(selectedService, discoveryListener)
            jmDNS?.addServiceTypeListener(discoveryTypeListener)

            discoverServiceName = selectedService

            discoverLogHandler?.invoke("discoverRegisterService - $selectedService - Register Service\n")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG_VC_MAIN_DISC_REG, "discoverRegisterService", e)

            discoverServiceName = null

            discoverLogHandler?.invoke("discoverRegisterService - $selectedService - ${e.message}\n\ndiscoverRegisterService - Stop Discovery\n")
            discoverLogHandler = null
        } catch (e: Exception) {
            Log.e(TAG_VC_MAIN_DISC_REG, "discoverRegisterService", e)

            discoverServiceName = null

            discoverLogHandler?.invoke("discoverRegisterService - $selectedService - ${e.message}\n\ndiscoverRegisterService - Stop Discovery\n")
            discoverLogHandler = null
        }
    }

    private fun discoverUnregisterService() {
        val selectedService = discoverServiceName
        try {
            jmDNS?.removeServiceTypeListener(discoveryTypeListener)
            jmDNS?.removeServiceListener(selectedService, discoveryListener)

            discoverServiceName = null

            discoverLogHandler?.invoke("discoverUnregisterService - $selectedService - Unregister Service\n")
            discoverLogHandler = null
        } catch (e: IllegalArgumentException) {
            Log.e(TAG_VC_MAIN_DISC_REG, "discoverUnregisterService", e)

            discoverServiceName = null

            discoverLogHandler?.invoke("discoverUnregisterService - $selectedService - ${e.message}\n\ndiscoverRegisterService - Stop Discovery\n")
            discoverLogHandler = null
        } catch (e: Exception) {
            Log.e(TAG_VC_MAIN_DISC_REG, "discoverUnregisterService", e)

            discoverServiceName = null

            discoverLogHandler?.invoke("discoverUnregisterService - $selectedService - ${e.message}\n\ndiscoverRegisterService - Stop Discovery\n")
            discoverLogHandler = null
        }
    }

    // Instantiate a new DiscoveryListener
    private val discoveryListener = object : ServiceListener {
        override fun serviceAdded(event: ServiceEvent) {
            Log.d(TAG_VC_MAIN_DISC_REG, "serviceAdded")
            Log.d(TAG_VC_MAIN_DISC_REG, event.info.toString())

            discoverLogHandler?.invoke("serviceAdded - ${event.info.toString()}")
        }

        override fun serviceRemoved(event: ServiceEvent) {
            Log.d(TAG_VC_MAIN_DISC_REG, "serviceRemoved")
            Log.d(TAG_VC_MAIN_DISC_REG, event.info.toString())

            discoverLogHandler?.invoke("serviceRemoved - ${event.info.toString()}")
        }

        override fun serviceResolved(event: ServiceEvent) {
            Log.d(TAG_VC_MAIN_DISC_REG, "serviceResolved")
            Log.d(TAG_VC_MAIN_DISC_REG, event.info.toString())

            discoverLogHandler?.invoke("serviceRemoved - ${event.info.toString()}")
        }
    }
    private val discoveryTypeListener = object : ServiceTypeListener {
        override fun serviceTypeAdded(event: ServiceEvent?) {
            Log.d(TAG_VC_MAIN_DISC_REG, "serviceTypeAdded")
            Log.d(TAG_VC_MAIN_DISC_REG, event?.info.toString())

            discoverLogHandler?.invoke("serviceTypeAdded - ${event?.info.toString()}")
        }

        override fun subTypeForServiceTypeAdded(event: ServiceEvent?) {
            Log.d(TAG_VC_MAIN_DISC_REG, "subTypeForServiceTypeAdded")
            Log.d(TAG_VC_MAIN_DISC_REG, event?.info.toString())

            discoverLogHandler?.invoke("subTypeForServiceTypeAdded - ${event?.info.toString()}")
        }
    }

    // Mark: - Wifi Multicast

    fun setupMulticast() {
        this.multicastLock = context?.applicationContext?.setupWifiMulticast()
    }

    fun destroyMulticast() {
        this.multicastLock?.release();
    }
}