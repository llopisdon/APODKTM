package com.machineinteractive.apodktm

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.machineinteractive.apodktm.databinding.FragmentApodsListBinding
import com.machineinteractive.apodktm.databinding.ListItemApodBinding


class ApodsListFragment : Fragment(), ApodAdapter.Listener {

    private var _binding: FragmentApodsListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApodsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.run {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = ApodAdapter(this@ApodsListFragment)
        }
    }

    override fun onClick(view: View) {
        // TODO
    }
}

class ApodDiffCallback : DiffUtil.ItemCallback<Apod>() {
    override fun areItemsTheSame(oldItem: Apod, newItem: Apod): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Apod, newItem: Apod): Boolean {
        return oldItem.date == newItem.date
    }
}

class ApodAdapter(private val listener: ApodAdapter.Listener) : ListAdapter<Apod, ApodAdapter.ViewHolder>(ApodDiffCallback()) {

    interface Listener {
        fun onClick(view: View)
    }

    inner class ViewHolder(private val binding: ListItemApodBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apod.setOnClickListener {
                listener.onClick(it)
            }
        }

        fun bind(apod: Apod) {
            binding.title.text = apod.title
            binding.image.load(apod.url)
            binding.apod.tag = apod.date
            binding.apod.transitionName = "apod_card_${apod.date}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ListItemApodBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

