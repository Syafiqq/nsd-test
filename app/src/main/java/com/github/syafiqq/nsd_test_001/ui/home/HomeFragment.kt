package com.github.syafiqq.nsd_test_001.ui.home

import android.content.Context.NSD_SERVICE
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.syafiqq.nsd_test_001.databinding.FragmentHomeBinding
import com.github.syafiqq.nsd_test_001.helper.setupWifiMulticast

private const val TAG_VC_MAIN_BROAD_REG = "VC-Main-Broad-Reg"
private const val TAG_VC_MAIN_DISC_REG = "VC-Main-Disc-Reg"
private const val TAG_VC = "VC-Home-Frag"

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private var nsdManager: NsdManager? = null

    private var discoverLogHandler: ((String) -> Unit)? = null
    private var discoverServiceName: String? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d(TAG_VC, "onCreateView")

        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
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
        nsdManager = context?.getSystemService(NSD_SERVICE) as? NsdManager
    }

    private fun broadcastRegisterService(port: Int) {
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo().apply {
            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceName = "NsdChat"
            serviceType = "_nsdchat._tcp"
            setPort(port)
        }

        try {
            nsdManager?.apply {
                registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, broadcastingListener)
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG_VC_MAIN_BROAD_REG, "broadcastRegisterService", e)
        }
    }

    private fun broadcastUnregisterService() {
        try {
            nsdManager?.unregisterService(broadcastingListener)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG_VC_MAIN_BROAD_REG, "broadcastUnregisterService")
        }
    }

    private val broadcastingListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            Log.d(TAG_VC_MAIN_BROAD_REG, "onServiceRegistered")
            Log.d(TAG_VC_MAIN_BROAD_REG, serviceInfo.toSimpleString())
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.d(TAG_VC_MAIN_BROAD_REG, "onRegistrationFailed - $errorCode")
            Log.d(TAG_VC_MAIN_BROAD_REG, serviceInfo.toSimpleString())
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            Log.d(TAG_VC_MAIN_BROAD_REG, "onServiceUnregistered")
            Log.d(TAG_VC_MAIN_BROAD_REG, serviceInfo.toSimpleString())
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.d(TAG_VC_MAIN_BROAD_REG, "onUnregistrationFailed - $errorCode")
            Log.d(TAG_VC_MAIN_BROAD_REG, serviceInfo.toSimpleString())
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
            nsdManager?.discoverServices(
                selectedService, NsdManager.PROTOCOL_DNS_SD, discoveryListener
            )
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
            nsdManager?.stopServiceDiscovery(discoveryListener)

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
    private val discoveryListener = object : NsdManager.DiscoveryListener {
        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG_VC_MAIN_DISC_REG, "onDiscoveryStarted")

            discoverLogHandler?.invoke("onDiscoveryStarted - $regType")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(TAG_VC_MAIN_DISC_REG, "onServiceFound - ${service.toSimpleString()}")

            discoverLogHandler?.invoke("onServiceFound - ${service.toSimpleString()}")
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG_VC_MAIN_DISC_REG, "onServiceLost - ${service.toSimpleString()}")

            discoverLogHandler?.invoke("onServiceLost - ${service.toSimpleString()}")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG_VC_MAIN_DISC_REG, "onDiscoveryStopped - $serviceType")

            discoverLogHandler?.invoke("onDiscoveryStopped - $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG_VC_MAIN_DISC_REG, "onStartDiscoveryFailed - $serviceType - $errorCode")

            discoverLogHandler?.invoke("onStartDiscoveryFailed - $serviceType - $errorCode")

            discoverUnregisterService()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG_VC_MAIN_DISC_REG, "onStopDiscoveryFailed - $serviceType - $errorCode")

            discoverLogHandler?.invoke("onStopDiscoveryFailed - $serviceType - $errorCode")

            discoverUnregisterService()
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

private fun NsdServiceInfo.toSimpleString(): String {
    return "ServiceName: $serviceName, ServiceType: $serviceType, Host: ${printHost()}, Port: $port"
}

private fun NsdServiceInfo.printHost(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(
            Build.VERSION_CODES.TIRAMISU
        ) >= 7
    ) {
        hostAddresses.map({ it.toString() }).joinToString()
    } else {
        host?.toString() ?: "-"
    }
}
