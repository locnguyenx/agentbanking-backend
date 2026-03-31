# New Requirement
## Overview

This requirement is to add up to auth & iam service.

## New Functional Requirements

### FR-1: User Management
- **FR-1.6** System shall support manage 2 type of user accounts: internal (for bank staff) and external (for agent). Each agent has only 1 user account, auto created when the agent account is created. Each staff may have only 1 user account or don't have user account.
- **FR-1.7** System shall support sending temporary password to user's email or mobile phone when user account is created. 
- **FR-1.8** System shall support user to reset password in case the user forget password, and sending new temporary password to user's email or mobile phone. 
- **FR-1.9** The temporary password policy: This temporary password will be expired after x days (configurable in system parameters). The user must change password after first login with temporary password.

### FR-3: Authorization & Access Control
- **FR-3.7** Permission restrict: Internal user can be provided permission to access only backoffice functions. External user can be provided permission to access only agent/merchant transaction functions.