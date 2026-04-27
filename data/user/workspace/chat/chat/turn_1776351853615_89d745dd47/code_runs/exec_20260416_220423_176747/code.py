import numpy as np
import matplotlib.pyplot as plt
from sklearn import svm, datasets

# Generate some linearly separable data
X, y = datasets.make_blobs(n_samples=100, centers=2, random_state=6, cluster_std=0.60)

# Create and fit the SVM model
clf = svm.SVC(kernel='linear')
clf.fit(X, y)

# Plot the data points
plt.scatter(X[:, 0], X[:, 1], c=y, cmap='autumn')

# Plot the decision boundary
ax = plt.gca()
xlim = ax.get_xlim()
ylim = ax.get_ylim()

# Create grid to evaluate model
xx = np.linspace(xlim[0], xlim[1], 200)
yy = np.linspace(ylim[0], ylim[1], 200)
YY, XX = np.meshgrid(yy, xx)
xy = np.vstack([XX.ravel(), YY.ravel()]).T
Z = clf.decision_function(xy).reshape(XX.shape)

# Plot decision boundary and margins
ax.contour(XX, YY, Z, colors='k', levels=[-1, 0, 1], alpha=0.5, linestyles=['--', '-', '--'])

# Highlight support vectors
ax.scatter(clf.support_vectors_[:, 0], clf.support_vectors_[:, 1], s=100,
           linewidth=1, facecolors='none', edgecolors='k')

plt.title('SVM with linear kernel')
plt.xlabel('Feature 1')
plt.ylabel('Feature 2')
plt.savefig('svm_example.png')
plt.show()