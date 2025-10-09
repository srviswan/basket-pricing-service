# Git Authentication Issues - GitLab/GitHub with IntelliJ

## 🔍 **The Problem**

You're experiencing authentication issues when pushing to GitLab/GitHub remote repository where:
- **Local commits** are authored by your personal ID
- **IntelliJ** is configured with a project token
- **Command line** push fails due to authentication mismatch

## 🎯 **Solutions**

### **Solution 1: Use IntelliJ to Push** ⭐ **EASIEST**

Since IntelliJ already has the project token configured:

1. **In IntelliJ**:
   - Go to `Git` → `Commit`
   - Or use `Cmd+K` (Mac) / `Ctrl+K` (Windows)
   - Review your changes
   - Click `Commit and Push`

2. **Or use VCS menu**:
   - `VCS` → `Git` → `Push...`
   - Or use `Cmd+Shift+K` (Mac) / `Ctrl+Shift+K` (Windows)

**This will use IntelliJ's configured token automatically.**

---

### **Solution 2: Configure Git Credential Helper**

Tell Git to use the same credentials as IntelliJ:

#### **For macOS (Keychain)**:
```bash
# Use macOS Keychain to store credentials
git config --global credential.helper osxkeychain

# Next time you push, Git will use stored credentials
git push origin main
```

#### **For Linux**:
```bash
# Use Git credential cache
git config --global credential.helper cache

# Or store permanently
git config --global credential.helper store
```

#### **For Windows**:
```bash
# Use Windows Credential Manager
git config --global credential.helper wincred
```

---

### **Solution 3: Use Personal Access Token**

Create and use your own personal access token for command line:

#### **GitHub**:
1. Go to: https://github.com/settings/tokens
2. Click `Generate new token (classic)`
3. Select scopes: `repo` (full control)
4. Generate and copy the token

#### **GitLab**:
1. Go to: https://gitlab.com/-/profile/personal_access_tokens
2. Create new token
3. Select scopes: `api`, `write_repository`
4. Generate and copy the token

#### **Use the token**:
```bash
# Set up credential helper first
git config --global credential.helper osxkeychain  # or cache/store

# Push - it will prompt for credentials
git push origin main

# Username: your-username
# Password: <paste your personal access token>

# Future pushes will use cached credentials
```

---

### **Solution 4: Update Remote URL with Token**

Embed the token in the remote URL (less secure, but works):

```bash
# Get current remote URL
git remote -v

# Update remote URL with token
git remote set-url origin https://<TOKEN>@github.com/srviswan/basket-pricing-service.git

# Or for GitLab
git remote set-url origin https://<TOKEN>@gitlab.com/workspace/repo.git

# Now push works without prompting
git push origin main
```

**⚠️ Warning**: This stores the token in plain text in `.git/config`

---

### **Solution 5: Use SSH Instead of HTTPS**

Switch to SSH authentication (more secure):

#### **Step 1: Generate SSH key** (if you don't have one)
```bash
# Check if you have an SSH key
ls -la ~/.ssh/

# If not, generate one
ssh-keygen -t ed25519 -C "your-email@example.com"

# Or use RSA if ed25519 is not supported
ssh-keygen -t rsa -b 4096 -C "your-email@example.com"
```

#### **Step 2: Add SSH key to GitHub/GitLab**

**GitHub**:
1. Copy your public key: `cat ~/.ssh/id_ed25519.pub`
2. Go to: https://github.com/settings/keys
3. Click `New SSH key`
4. Paste the key and save

**GitLab**:
1. Copy your public key: `cat ~/.ssh/id_ed25519.pub`
2. Go to: https://gitlab.com/-/profile/keys
3. Add new SSH key
4. Paste and save

#### **Step 3: Update Git remote to use SSH**

```bash
# Check current remote
git remote -v

# Update to SSH (GitHub)
git remote set-url origin git@github.com:srviswan/basket-pricing-service.git

# Update to SSH (GitLab)
git remote set-url origin git@gitlab.com:workspace/repo.git

# Test connection
ssh -T git@github.com  # or git@gitlab.com

# Now push works
git push origin main
```

---

### **Solution 6: Match IntelliJ Configuration**

Configure command line Git to use the same token as IntelliJ:

#### **Step 1: Find IntelliJ's token**
In IntelliJ:
- `Settings` → `Version Control` → `GitHub` / `GitLab`
- Copy the token

#### **Step 2: Configure Git credential helper**
```bash
# Use credential helper
git config --global credential.helper osxkeychain  # macOS
git config --global credential.helper cache        # Linux

# Try to push
git push origin main

# When prompted:
# Username: <your-username-or-token-name>
# Password: <paste-the-token-from-intellij>
```

---

## 🔧 **Quick Fix for Your Specific Issue**

Since you're working on this project and IntelliJ already has the correct token:

### **Immediate Solution:**

**Option A: Push from IntelliJ**
1. Open IntelliJ
2. Press `Cmd+Shift+K` (Mac) or `Ctrl+Shift+K` (Windows)
3. Click `Push`

**Option B: Get the token and use it in command line**
1. In IntelliJ: `Settings` → `Version Control` → `GitHub`
2. Copy the token
3. In terminal:
   ```bash
   git config --global credential.helper osxkeychain
   git push origin main
   # Username: srviswan (or token name)
   # Password: <paste token>
   ```

**Option C: Use SSH (one-time setup)**
1. Generate SSH key: `ssh-keygen -t ed25519 -C "your-email@example.com"`
2. Add to GitHub: https://github.com/settings/keys
3. Change remote: `git remote set-url origin git@github.com:srviswan/basket-pricing-service.git`
4. Push: `git push origin main`

---

## 🚫 **Common Errors and Fixes**

### **Error: "Support for password authentication was removed"**
```
remote: Support for password authentication was removed on August 13, 2021.
remote: Please use a personal access token instead.
```

**Fix**: Use a personal access token instead of password
```bash
git push origin main
# Username: your-username
# Password: <your-personal-access-token>  # NOT your password!
```

---

### **Error: "Authentication failed"**
```
fatal: Authentication failed for 'https://github.com/...'
```

**Fix**: Update stored credentials
```bash
# Clear old credentials
git credential-osxkeychain erase
host=github.com
protocol=https
<press Enter twice>

# Try again - will prompt for new credentials
git push origin main
```

---

### **Error: "Permission denied (publickey)"** (SSH)
```
Permission denied (publickey).
fatal: Could not read from remote repository.
```

**Fix**: Add your SSH key to GitHub/GitLab
```bash
# Copy your public key
cat ~/.ssh/id_ed25519.pub

# Add it to: https://github.com/settings/keys
```

---

## 📋 **Recommended Approach**

### **For Personal Projects:**
Use SSH (more secure, no token management):
```bash
git remote set-url origin git@github.com:srviswan/basket-pricing-service.git
git push origin main
```

### **For Corporate Projects:**
Use project token via credential helper:
```bash
git config --global credential.helper osxkeychain
git push origin main  # Enter token when prompted
```

### **For CI/CD:**
Use deploy tokens or service accounts with minimal permissions.

---

## 🔍 **Debug Your Current Setup**

```bash
# 1. Check current remote URL
git remote -v

# 2. Check Git config
git config --list | grep credential

# 3. Check if SSH key exists
ls -la ~/.ssh/

# 4. Test GitHub SSH connection
ssh -T git@github.com

# 5. Check stored credentials (macOS)
git credential-osxkeychain get
host=github.com
protocol=https
<press Enter>
```

---

## 🎯 **Quick Commands Reference**

```bash
# Switch to SSH
git remote set-url origin git@github.com:srviswan/basket-pricing-service.git

# Switch to HTTPS with token
git remote set-url origin https://github.com/srviswan/basket-pricing-service.git

# Use credential helper (macOS)
git config --global credential.helper osxkeychain

# Clear credentials
git credential-osxkeychain erase
host=github.com
protocol=https

# Push
git push origin main

# Push with force (be careful!)
git push origin main --force  # Only if absolutely necessary

# Push specific branch
git push origin <branch-name>
```

---

## 💡 **Recommendation for Your Case**

Since IntelliJ is already configured correctly:

**Easiest**: Just push from IntelliJ (`Cmd+Shift+K`)

**Best long-term**: Set up SSH keys (one-time setup, works everywhere)
```bash
# 1. Generate key
ssh-keygen -t ed25519 -C "your-email@example.com"

# 2. Add to GitHub
cat ~/.ssh/id_ed25519.pub  # Copy this
# Add at: https://github.com/settings/keys

# 3. Update remote
git remote set-url origin git@github.com:srviswan/basket-pricing-service.git

# 4. Push (will work from now on)
git push origin main
```

This way, both IntelliJ and command line will work seamlessly! 🚀

