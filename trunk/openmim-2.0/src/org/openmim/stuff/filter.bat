grep -ai "network error" *log* > ne
grep -av "Recod.* Your status is now OFFLINE" ne > ne00
grep -a  "2002" ne00 > ne00.2002


grep -ai "exception" *log* | grep -avi "network error\|is probably used on another\|Invalid password\|Session.handleExceptio\|Cannot perform this operation while\|ection refused to host: 194.149.226.29; nested exc" > ex
grep -a  "Exception during server relook\|ConnectException: Connection refuse" ex > "ex.connection refused"
grep -av "Exception during server relook\|ConnectException: Connection refuse" ex > ex00
grep -av "server refuses to return the user details" ex00 > ex01

grep -ai "search" ex01>ex01.search
grep -ai "get userinfo for .*: response timeout expired" ex01 | grep -av "MessagingNetworkException: can't get userinfo" >"ex01.getinfo timeout"
grep -ai "userinfo" ex01 | grep -av "get userinfo for .*: response timeout expired" >"ex01.getinfo other"
grep -a  "Unknown authorization error" ex01 | grep -av "MessagingNetworkException: Cannot login" >"ex01.unknown auth error"
grep -ai "auth" ex01 | grep -av "Unknown authorization error\|my authorizationRequired assumed as false\|userinfo\|Exception during automatic .authorization granted. send. Ignoring" >ex01.auth
grep -ai "connect rate" ex01>"ex01.connect rate"

grep -a  "StringIndexOutOfBoundsException" ex01>"ex01.x stringIndexOutOfBoundsException"
grep -a  "Remote" ex01>ex01.x remote
grep -ai "SmscApi\|smsc api" ex01 |grep -via "trying again" | sort > "ex01.x smscapi
grep -ai "ioexception\|connectexception" ex01>"ex01.x ioexception"
grep -ai "not implemented" ex01>"ex01.x not implemented"

grep -aiv "search\|userinfo\|auth\|StringIndexOutOfBoundsException\|connect rate\|ioexception\|connectexception\|smscapi\|smsc api\|not implemented" ex01 | grep -av "Remote" >"ex01.xx other"
