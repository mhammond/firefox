/* -*- Mode: C++; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* vim: set ts=8 sts=2 et sw=2 tw=80: */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef mozilla_extensions_StreamFilterParent_h
#define mozilla_extensions_StreamFilterParent_h

#include "StreamFilterBase.h"
#include "mozilla/extensions/PStreamFilterParent.h"

#include "mozilla/LinkedList.h"
#include "mozilla/Mutex.h"
#include "mozilla/WebRequestService.h"
#include "nsIStreamListener.h"
#include "nsIThread.h"
#include "nsIThreadRetargetableStreamListener.h"
#include "nsThreadUtils.h"

#if defined(_MSC_VER)
#  define FUNC __FUNCSIG__
#else
#  define FUNC __PRETTY_FUNCTION__
#endif

namespace mozilla {
namespace dom {
class ContentParent;
}
namespace net {
class ChannelEventQueue;
class nsHttpChannel;
}  // namespace net

namespace extensions {

using namespace mozilla::dom;
using mozilla::ipc::IPCResult;

class StreamFilterParent final : public PStreamFilterParent,
                                 public nsIThreadRetargetableStreamListener,
                                 public nsIRequest,
                                 public StreamFilterBase {
  friend class PStreamFilterParent;

 public:
  NS_DECL_THREADSAFE_ISUPPORTS
  NS_DECL_NSISTREAMLISTENER
  NS_DECL_NSIREQUEST
  NS_DECL_NSIREQUESTOBSERVER
  NS_DECL_NSITHREADRETARGETABLESTREAMLISTENER

  StreamFilterParent();

  using ParentEndpoint = mozilla::ipc::Endpoint<PStreamFilterParent>;
  using ChildEndpoint = mozilla::ipc::Endpoint<PStreamFilterChild>;

  using ChildEndpointPromise = MozPromise<ChildEndpoint, bool, true>;

  [[nodiscard]] static RefPtr<ChildEndpointPromise> Create(
      ContentParent* aContentParent, uint64_t aChannelId,
      const nsAString& aAddonId);

  static void Attach(nsIChannel* aChannel, ParentEndpoint&& aEndpoint);

  enum class State {
    // The parent has been created, but not yet constructed by the child.
    Uninitialized,
    // The parent has been successfully constructed.
    Initialized,
    // The OnRequestStarted event has been received, and data is being
    // transferred to the child.
    TransferringData,
    // The channel is suspended.
    Suspended,
    // The channel has been closed by the child, and will send or receive data.
    Closed,
    // The channel is being disconnected from the child, so that all further
    // data and events pass unfiltered to the output listener. Any data
    // currnetly in transit to, or buffered by, the child will be written to the
    // output listener before we enter the Disconnected atate.
    Disconnecting,
    // The channel has been disconnected from the child, and all further data
    // and events will be passed directly to the output listener.
    Disconnected,
  };

  // This method makes StreamFilterParent to disconnect from channel.
  // Notice that this method can only be called before OnStartRequest().
  void Disconnect(const nsACString& aReason);

 protected:
  virtual ~StreamFilterParent();

  IPCResult RecvWrite(Data&& aData);
  IPCResult RecvFlushedData();
  IPCResult RecvSuspend();
  IPCResult RecvResume();
  IPCResult RecvClose();
  IPCResult RecvDisconnect();
  IPCResult RecvDestroy();

 private:
  bool IPCActive() {
    return (mState != State::Closed && mState != State::Disconnecting &&
            mState != State::Disconnected);
  }

  void Init(nsIChannel* aChannel);

  void Bind(ParentEndpoint&& aEndpoint);

  void Destroy();

  nsresult FlushBufferedData();

  nsresult Write(Data& aData);

  void WriteMove(Data&& aData);

  void DoSendData(Data&& aData);

  nsresult EmitStopRequest(nsresult aStatusCode);

  virtual void ActorDestroy(ActorDestroyReason aWhy) override;

  void Broken();
  void FinishDisconnect();

  void CheckResult(bool aResult) {
    if (NS_WARN_IF(!aResult)) {
      Broken();
    }
  }

  inline nsIEventTarget* ActorThread();

  inline nsISerialEventTarget* IOThread();

  inline bool IsIOThread();

  inline bool IsActorThread();

  inline void AssertIsActorThread();

  inline void AssertIsIOThread();

  static void AssertIsMainThread() { MOZ_ASSERT(NS_IsMainThread()); }

  template <typename Function>
  void RunOnMainThread(const char* aName, Function&& aFunc);

  void RunOnMainThread(already_AddRefed<Runnable> aRunnable);

  template <typename Function>
  void RunOnActorThread(const char* aName, Function&& aFunc);

  template <typename Function>
  void RunOnIOThread(const char* aName, Function&& aFunc);

  void RunOnIOThread(already_AddRefed<Runnable>);

  nsCOMPtr<nsIChannel> mChannel;
  nsCOMPtr<nsILoadGroup> mLoadGroup;
  nsCOMPtr<nsIStreamListener> mOrigListener;

  nsCOMPtr<nsISerialEventTarget> mMainThread;
  nsCOMPtr<nsISerialEventTarget> mIOThread;

  RefPtr<net::ChannelEventQueue> mQueue;

  Mutex mBufferMutex MOZ_UNANNOTATED;

  bool mReceivedStop;
  bool mSentStop;
  bool mDisconnected = false;

  // If redirection happens or alterate cached data is being sent, the stream
  // filter is disconnected in OnStartRequest and the following ODA would not
  // be filtered. Using mDisconnected causes race condition. mState is possible
  // to late to be set, which leads out of sync.
  bool mDisconnectedByOnStartRequest = false;

  // When in mState Disconnecting, data from ODA is buffered, and flushed when
  // FinishDisconnect() is called. After this, mState is set to Disconnected,
  // and any subsequent data to ODA are written directly to mOrigListener.
  // Between flushing the data (on the IO thread) and updating mState (on the
  // actor thread), it is theoretically possible to receive another ODA (on the
  // IO thread). This flag here ensures that ODA knows that data should be
  // written instead of buffered.
  bool mDisconnectedByFinishDisconnect = false;

  bool mBeforeOnStartRequest = true;

  nsCOMPtr<nsISupports> mContext;
  uint64_t mOffset;

  // Use Release-Acquire ordering to ensure the OMT ODA is not sent while
  // the channel is disconnecting or closed.
  Atomic<State, ReleaseAcquire> mState;
};

}  // namespace extensions
}  // namespace mozilla

#endif  // mozilla_extensions_StreamFilterParent_h
