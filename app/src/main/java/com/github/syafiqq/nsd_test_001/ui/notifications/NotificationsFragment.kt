package com.github.syafiqq.nsd_test_001.ui.notifications

import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.druk.dnssd.BrowseListener
import com.github.druk.dnssd.DNSSD
import com.github.druk.dnssd.DNSSDBindable
import com.github.druk.dnssd.DNSSDRegistration
import com.github.druk.dnssd.DNSSDService
import com.github.druk.dnssd.RegisterListener
import com.github.syafiqq.nsd_test_001.databinding.FragmentNotificationsBinding
import com.github.syafiqq.nsd_test_001.helper.setupWifiMulticast

private const val TAG_VC_MAIN_BROAD_REG = "VC-Main-Broad-Reg"
private const val TAG_VC_MAIN_DISC_REG = "VC-Main-Disc-Reg"
private const val TAG_VC = "VC-Notifications-Frag"

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private var dbssd: DNSSD? = null

    private var discoverLogHandler: ((String) -> Unit)? = null
    private var discoverServiceName: String? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d(TAG_VC, "onCreateView")

        val notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
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
        val context = context
        if (context != null) {
            dbssd = DNSSDBindable(context);
        } else {
            dbssd = null
        }
    }

    private fun broadcastRegisterService(port: Int) {
        try {
            dbssd?.register("NsdChat", "_nsdchat._tcp", port, broadcastingListener)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG_VC_MAIN_BROAD_REG, "broadcastRegisterService", e)
        }
    }

    private fun broadcastUnregisterService() {
        try {
        } catch (e: IllegalArgumentException) {
            Log.e(TAG_VC_MAIN_BROAD_REG, "broadcastUnregisterService")
        }
    }

    private val broadcastingListener = object : RegisterListener {
        override fun serviceRegistered(
            registration: DNSSDRegistration?,
            flags: Int,
            serviceName: String?,
            regType: String?,
            domain: String?
        ) {
            Log.d(TAG_VC_MAIN_BROAD_REG, "serviceRegistered")
            Log.d(TAG_VC_MAIN_BROAD_REG, "$flags - $serviceName - $regType - $domain")
        }

        override fun operationFailed(
            service: DNSSDService?, errorCode: Int
        ) {
            Log.d(TAG_VC_MAIN_BROAD_REG, "operationFailed")
            Log.d(TAG_VC_MAIN_BROAD_REG, "$errorCode")
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
            dbssd?.browse(selectedService, discoveryListener)

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
    private val discoveryListener = object : BrowseListener {
        override fun serviceFound(
            browser: DNSSDService?,
            flags: Int,
            ifIndex: Int,
            serviceName: String?,
            regType: String?,
            domain: String?
        ) {
            Log.e(
                TAG_VC_MAIN_DISC_REG,
                "serviceFound - $flags - $ifIndex - $serviceName - $regType - $domain"
            )

            discoverLogHandler?.invoke("serviceFound - $flags - $ifIndex - $serviceName - $regType - $domain")
        }

        override fun serviceLost(
            browser: DNSSDService?,
            flags: Int,
            ifIndex: Int,
            serviceName: String?,
            regType: String?,
            domain: String?
        ) {
            Log.e(
                TAG_VC_MAIN_DISC_REG,
                "serviceLost - $flags - $ifIndex - $serviceName - $regType - $domain"
            )

            discoverLogHandler?.invoke("serviceLost - $flags - $ifIndex - $serviceName - $regType - $domain")
        }

        override fun operationFailed(
            service: DNSSDService?, errorCode: Int
        ) {
            Log.e(TAG_VC_MAIN_DISC_REG, "operationFailed - $errorCode")

            discoverLogHandler?.invoke("operationFailed - $errorCode")
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