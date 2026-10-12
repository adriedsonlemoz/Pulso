package br.com.pulsobateria.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.pulsobateria.PulsoApp
import br.com.pulsobateria.data.SessionSample
import br.com.pulsobateria.data.Settings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PulsoViewModel(application: Application): AndroidViewModel(application) {
 private val app = application as PulsoApp
 val reading = app.repository.reading
 fun sessionSamples(sessionId: Long): Flow<List<SessionSample>> = app.repository.sessionSamples(sessionId)
 val sessions = app.repository.sessions.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val settings = app.settings.flow.stateIn(viewModelScope,SharingStarted.Eagerly,Settings())
 val active = app.repository.serviceActive
 val error = app.repository.serviceError
 fun theme(value: String) { viewModelScope.launch { app.settings.theme(value) } }
 fun interval(value: Int) { viewModelScope.launch { app.settings.interval(value) } }
 fun fahrenheit(value: Boolean) { viewModelScope.launch { app.settings.fahrenheit(value) } }
}
