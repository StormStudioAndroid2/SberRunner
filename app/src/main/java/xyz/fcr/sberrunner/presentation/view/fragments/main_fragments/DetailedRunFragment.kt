package xyz.fcr.sberrunner.presentation.view.fragments.main_fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import xyz.fcr.sberrunner.data.model.Run
import xyz.fcr.sberrunner.databinding.FragmentDetailedRunBinding
import xyz.fcr.sberrunner.presentation.App
import xyz.fcr.sberrunner.presentation.viewmodels.main_viewmodels.DetailedRunViewModel
import xyz.fcr.sberrunner.utils.*
import xyz.fcr.sberrunner.utils.Constants.CURRENT_RUN_ID
import java.text.SimpleDateFormat
import javax.inject.Inject

class DetailedRunFragment : Fragment() {
    private var _binding: FragmentDetailedRunBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    val viewModel: DetailedRunViewModel by viewModels { factory }

    init {
        App.appComponent.inject(this)
    }

    private var isMetric = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailedRunBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = this.arguments

        if (bundle != null && bundle.containsKey(CURRENT_RUN_ID)) {
            val runID = bundle.getInt(CURRENT_RUN_ID)
            viewModel.getRunFromDB(runID)
        }

        binding.detailedBackButton.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        observeLiveData()
    }


    private fun observeLiveData() {
        viewModel.runLiveData.observe(viewLifecycleOwner) { run: Run -> showRunDetailsInfo(run)}
        viewModel.unitsLiveData.observe(viewLifecycleOwner) { units: Boolean -> isMetric = units }
    }

    @SuppressLint("SimpleDateFormat")
    private fun showRunDetailsInfo(run: Run) {
        val sdfDate = SimpleDateFormat("dd, MMM, yyyy HH:mm")
        binding.detailedDateOfRunTv.text = sdfDate.format(run.timestamp)

        if (isMetric) {
            binding.detailedDistance.text = run.distanceInMeters
                .toString()
                .addDistanceUnits(isMetric)
        } else {
            binding.detailedDistance.text = run.distanceInMeters
                .convertMetersToMiles()
                .toString()
                .addDistanceUnits(isMetric)
        }

        binding.detailedTime.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

        if (isMetric) {
            binding.detailedSpeed.text = run.avgSpeedInKMH
                .toString()
                .addSpeedUnits(isMetric)
        } else {
            binding.detailedSpeed.text = run.avgSpeedInKMH
                .convertKMHtoMPH()
                .toString()
                .addSpeedUnits(isMetric)
        }

        binding.detailedCalories.text = run.calories
            .toString()
            .addCalories()

        Glide.with(this)
            .load(run.mapImage)
            .apply(RequestOptions.bitmapTransform(RoundedCorners(40)))
            .into(binding.detailedMap)
    }
}