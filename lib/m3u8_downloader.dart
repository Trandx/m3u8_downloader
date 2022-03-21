import 'dart:async';
import 'dart:ui';

import 'package:flutter/services.dart';

import 'callback_dispatcher.dart';

typedef CallbackHandle? _GetCallbackHandle(Function callback);
typedef SelectNotificationCallback = Future<dynamic> Function();


class M3u8Downloader {
  static const MethodChannel _channel = const MethodChannel('vincent/m3u8_downloader', JSONMethodCodec());
  static _GetCallbackHandle _getCallbackHandle = (Function callback) => PluginUtilities.getCallbackHandle(callback);
  static SelectNotificationCallback? _onSelectNotification;
  static bool _initialized = false;


  /// Initialiser le téléchargeur
  /// Doit être appelé avant de pouvoir être utilisé.
  ///
  /// - [onSelect] Fonction de rappel pour les notifications de clics.
  static Future<bool> initialize({
    SelectNotificationCallback? onSelect
  }) async {
    assert(!_initialized, 'M3u8Downloader.initialize() must be called only once!');

    final CallbackHandle? handle = _getCallbackHandle(callbackDispatcher);
    if (handle == null) {
      return false;
    }
    if (onSelect != null) {
      _onSelectNotification = onSelect;
    }
    _channel.setMethodCallHandler((MethodCall call) {
      switch (call.method) {
        case 'selectNotification':
          if (_onSelectNotification == null) {
            return Future.value(false);
          }
          return _onSelectNotification!();
        default:
          return Future.error('method not defined');
      }
    });

    final bool? r = await _channel.invokeMethod<bool>('initialize',{
      "handle": handle.toRawHandle(),
    });
    _initialized = r ?? false;
    return _initialized;
  }

  /// Télécharger la configuration
  ///
  /// - [saveDir] Où enregistrer le fichier
  /// - [showNotification] Indique si les notifications doivent être affichées.
  /// - [convertMp4] s'il faut convertir en mp4
  /// - [connTimeout] le délai de connexion au réseau.
  /// - [readTimeout] le délai d'attente pour la lecture du fichier.
  /// - [threadCount] le nombre de threads qui téléchargent en même temps.
  /// - [debugMode] Mode de débogage 
  static Future<bool> config({
    String? saveDir,
    bool showNotification = true,
    bool convertMp4 = false,
    int? connTimeout,
    int? readTimeout,
    int? threadCount,
    bool? debugMode,
  }) async {
    final bool? r = await _channel.invokeMethod<bool>('config',{
      "saveDir": saveDir,
      "showNotification": showNotification,
      "convertMp4": convertMp4,
      "connTimeout": connTimeout,
      "readTimeout": readTimeout,
      "threadCount": threadCount,
      "debugMode": debugMode,
    });
    return r ?? false;
  }

  /// Télécharger le fichier
  /// 
  /// - [url] Adresse du lien de téléchargement
  /// - [name] Nom du fichier à télécharger (titre de la notification)
  /// - [progressCallback] Rappel de la progression du téléchargement.
  /// - [successCallback] Télécharger le rappel du succès.
  /// - [errorCallback] Rappel de l'échec du téléchargement.
  static void download({
    required String url,
    required String name,
    Function? progressCallback,
    Function? successCallback,
    Function? errorCallback
  }) async {
    assert(url.isNotEmpty && name.isNotEmpty);
    assert(_initialized, 'M3u8Downloader.initialize() must be called first!');

    Map<String, dynamic> params = {
      "url": url,
      "name": name,
    };
    if (progressCallback != null) {
      final CallbackHandle? handle = _getCallbackHandle(progressCallback);
      if (handle != null) {
        params["progressCallback"] = handle.toRawHandle();
      }
    }
    if (successCallback != null) {
      final CallbackHandle? handle = _getCallbackHandle(successCallback);
      if (handle != null) {
        params["successCallback"] = handle.toRawHandle();
      }
    }
    if (errorCallback != null) {
      final CallbackHandle? handle = _getCallbackHandle(errorCallback);
      if (handle != null) {
        params["errorCallback"] = handle.toRawHandle();
      }
    }

    await _channel.invokeMethod("download", params);
  }

  /// Pause téléchargement
  /// 
  /// - [url] pause l'adresse du lien spécifié
  static void pause(String url) async {
    assert(_initialized, 'M3u8Downloader.initialize() must be called first!');
    await _channel.invokeMethod("pause", {
      "url": url
    });
  }

  /// Supprimer le téléchargement
  /// 
  /// - [url] Adresse du lien de téléchargement
  static Future<bool> delete(String url) async {
    assert(url.isNotEmpty);
    assert(_initialized, 'M3u8Downloader.initialize() must be called first!');

    return await _channel.invokeMethod("delete", {
      "url": url
    }) ?? false;
  }

     /// État du téléchargement
  static Future<bool> isRunning() async {
    assert(_initialized, 'M3u8Downloader.initialize() must be called first!');
    bool isRunning = await _channel.invokeMethod("isRunning");
    return isRunning;
  }

   /// Obtenir le chemin vers la sauvegarde par URL
  /// - [url] l'URL demandée
  /// baseDir - chemin d'enregistrement du fichier de base
  /// m3u8 - adresse du fichier m3u8
  /// mp4 - emplacement où le mp4 est stocké
  static Future<dynamic> getSavePath(String url) async {
    return await _channel.invokeMethod("getSavePath", { "url": url });
  }
}
