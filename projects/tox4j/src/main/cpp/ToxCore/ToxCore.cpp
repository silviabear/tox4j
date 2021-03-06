#include "ToxCore.h"

#ifdef TOX_VERSION_MAJOR

using namespace core;

#include "generated/impls.h"

void
reference_symbols_tox ()
{
  int tox_finalize; // For Java only.
#include "generated/natives.h"
}

#define TOX_MAX_HOSTNAME_LENGTH 255
#define TOX_DEFAULT_PROXY_PORT  8080
#define TOX_DEFAULT_TCP_PORT    0
#define TOX_DEFAULT_START_PORT  33445
#define TOX_DEFAULT_END_PORT    (TOX_DEFAULT_START_PORT + 100)
#include "generated/constants.h"

ToxInstances<tox::core_ptr, std::unique_ptr<Events>> core::instances;

template<> extern char const *const module_name<Tox> = "core";
template<> extern char const *const exn_prefix<Tox> = "";

#endif
