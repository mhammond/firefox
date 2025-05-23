const SVG_NS = "http://www.w3.org/2000/svg";
const XHTML_NS = "http://www.w3.org/1999/xhtml";
const XUL_NS = "http://www.mozilla.org/keymaster/gatekeeper/there.is.only.xul";

function trackPromise(promise) {
  let isResolved = false;
  let isRejected = false;

  const trackedPromise = promise.then(
    value => {
      isResolved = true;
      return value;
    },
    error => {
      isRejected = true;
      throw error;
    }
  );

  trackedPromise.isResolved = () => isResolved;
  trackedPromise.isRejected = () => isRejected;
  trackedPromise.isPending = () => !isResolved && !isRejected;

  return trackedPromise;
}

const hasPromiseResolved = async function (promise) {
  let resolved = false;
  promise.finally(() => (resolved = true)).catch(() => {});
  // Make sure microtasks have time to run.
  await new Promise(resolve => Services.tm.dispatchToMainThread(resolve));
  return resolved;
};

const hasPromiseRejected = async function (promise) {
  let rejected = false;
  promise.catch(() => (rejected = true));
  // Make sure microtasks have time to run.
  await new Promise(resolve => Services.tm.dispatchToMainThread(resolve));
  return rejected;
};
