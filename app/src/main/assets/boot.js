(function () {
  'use strict';
  var finished = false;
  var timer;
  function element(tag, text) {
    var node = document.createElement(tag);
    if (text) node.textContent = text;
    return node;
  }
  // replaceChildren arrived later than the Android 8 WebView baseline.
  if (!Element.prototype.replaceChildren) {
    Element.prototype.replaceChildren = function () {
      while (this.firstChild) this.removeChild(this.firstChild);
      for (var i = 0; i < arguments.length; i++) {
        var item = arguments[i];
        this.appendChild(typeof item === 'string' ? document.createTextNode(item) : item);
      }
    };
  }
  function report(message) {
    if (finished) {
      if (window.CyberBoot.onRuntimeError) window.CyberBoot.onRuntimeError(message);
      return;
    }
    clearTimeout(timer);
    document.body.classList.remove('busy');
    var toolbar = document.getElementById('toolbar');
    var content = document.getElementById('content');
    toolbar.textContent = '赛博中枢管家';
    content.textContent = '';
    var box = element('div');
    box.className = 'bootStatus';
    box.appendChild(element('h2', '无法启动'));
    box.appendChild(element('p', '请重试。如果再次发生，请分享以下错误详情。'));
    var details = element('pre', '赛博中枢管家 0.1.12\n' + message + '\n\n' + navigator.userAgent);
    details.className = 'bootDetails';
    box.appendChild(details);
    var retry = element('button', '重试');
    retry.className = 'primary';
    retry.onclick = function () { window.location.reload(); };
    box.appendChild(retry);
    content.appendChild(box);
  }
  window.CyberBoot = {
    fail: function (error) { report(error && error.message ? error.message : String(error)); },
    ready: function () { finished = true; clearTimeout(timer); },
    onRuntimeError: null
  };
  window.addEventListener('error', function (event) {
    var target = event.target;
    if (target && target.tagName === 'SCRIPT') report('未能加载 ' + target.getAttribute('src'));
    else if (event.message) report(event.message + (event.lineno ? '（第 ' + event.lineno + ' 行）' : ''));
  }, true);
  window.addEventListener('unhandledrejection', function (event) {
    var reason = event.reason;
    report(reason && reason.message ? reason.message : String(reason));
  });
  var retry = document.getElementById('bootRetry');
  if (retry) retry.onclick = function () { window.location.reload(); };
  timer = setTimeout(function () { report('打开文件管理器时启动超时。'); }, 20000);
}());
