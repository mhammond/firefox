/* -*- Mode: C++; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* vim:expandtab:shiftwidth=2:tabstop=2:
 */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "nsUnixRemoteServer.h"
#include "nsGTKToolkit.h"
#include "nsCOMPtr.h"
#include "nsICommandLineRunner.h"
#include "nsCommandLine.h"
#include "nsIFile.h"

// Set desktop startup ID to the passed ID, if there is one, so that any created
// windows get created with the right window manager metadata, and any windows
// that get new tabs and are activated also get the right WM metadata.
// The timestamp will be used if there is no desktop startup ID, or if we're
// raising an existing window rather than showing a new window for the first
// time.
void nsUnixRemoteServer::SetStartupTokenOrTimestamp(
    const nsACString& aStartupToken, uint32_t aTimestamp) {
  nsGTKToolkit* toolkit = nsGTKToolkit::GetToolkit();
  if (!toolkit) {
    return;
  }

  if (!aStartupToken.IsEmpty()) {
    toolkit->SetStartupToken(aStartupToken);
  }

  toolkit->SetFocusTimestamp(aTimestamp);
}

static bool FindExtensionParameterInCommand(const char* aParameterName,
                                            const nsACString& aCommand,
                                            char aSeparator,
                                            nsACString* aValue) {
  nsAutoCString searchFor;
  searchFor.Append(aSeparator);
  searchFor.Append(aParameterName);
  searchFor.Append('=');

  nsACString::const_iterator start, end;
  aCommand.BeginReading(start);
  aCommand.EndReading(end);
  if (!FindInReadable(searchFor, start, end)) return false;

  nsACString::const_iterator charStart, charEnd;
  charStart = end;
  aCommand.EndReading(charEnd);
  nsACString::const_iterator idStart = charStart, idEnd;
  if (FindCharInReadable(aSeparator, charStart, charEnd)) {
    idEnd = charStart;
  } else {
    idEnd = charEnd;
  }
  *aValue = nsDependentCSubstring(idStart, idEnd);
  return true;
}

const char* nsUnixRemoteServer::HandleCommandLine(
    mozilla::Span<const char> aBuffer, uint32_t aTimestamp) {
  nsCOMPtr<nsICommandLineRunner> cmdline(new nsCommandLine());

  // the commandline property is constructed as an array of int32_t
  // followed by a series of null-terminated strings:
  //
  // [argc][offsetargv0][offsetargv1...]<workingdir>\0<argv[0]>\0argv[1]...\0
  // (offset is from the beginning of the buffer)
  if (aBuffer.size() < sizeof(uint32_t)) {
    return "500 command not parseable";
  }

  uint32_t argc =
      TO_LITTLE_ENDIAN32(*reinterpret_cast<const uint32_t*>(aBuffer.data()));
  uint32_t offsetFilelist = ((argc + 1) * sizeof(uint32_t));
  if (offsetFilelist >= aBuffer.size()) {
    return "500 command not parseable";
  }
  const char* workingDir = aBuffer.data() + offsetFilelist;

  nsCOMPtr<nsIFile> lf;
  nsresult rv =
      NS_NewNativeLocalFile(nsDependentCString(workingDir), getter_AddRefs(lf));
  if (NS_FAILED(rv)) {
    return "509 internal error";
  }

  const char** argv = (const char**)malloc(sizeof(char*) * argc);
  if (!argv) {
    return "509 internal error";
  }

  const uint32_t* offset =
      reinterpret_cast<const uint32_t*>(aBuffer.data()) + 1;
  nsAutoCString desktopStartupID;

  for (unsigned int i = 0; i < argc; ++i) {
    uint32_t argvOffset = TO_LITTLE_ENDIAN32(offset[i]);
    if (argvOffset >= aBuffer.size()) {
      return "500 command not parseable";
    }

    argv[i] = aBuffer.data() + argvOffset;

    if (i == 0) {
      nsDependentCString cmd(argv[0]);
      FindExtensionParameterInCommand("STARTUP_TOKEN", cmd, ' ',
                                      &desktopStartupID);
    }
  }

  rv = cmdline->Init(argc, argv, lf, nsICommandLine::STATE_REMOTE_AUTO);

  free(argv);
  if (NS_FAILED(rv)) {
    return "509 internal error";
  }

  SetStartupTokenOrTimestamp(desktopStartupID, aTimestamp);

  rv = cmdline->Run();

  if (NS_ERROR_ABORT == rv) {
    return "500 command not parseable";
  }

  if (NS_FAILED(rv)) {
    return "509 internal error";
  }

  return "200 executed command";
}
