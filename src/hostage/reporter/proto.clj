(ns hostage.reporter.proto)

(defprotocol Reporter
  (begin [this])

  (begin-step [this opts])
  (end-step [this opts])

  (end [this]))
