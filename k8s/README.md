# EliteBNB Kubernetes Local Deployment

These manifests run the EliteBNB Spring Boot backend and a local PostgreSQL 17 database in Minikube.

The example Secret file contains placeholders only. Create the real Secret locally with `kubectl create secret`; do not commit real credentials.

## 1. Verify Docker

```powershell
docker version
docker info
```

## 2. Verify Minikube

```powershell
minikube version
kubectl version --client
```

## 3. Start Minikube

```powershell
minikube start --driver=docker
kubectl cluster-info
```

## 4. Build the Docker image

Run this from the repository root:

```powershell
docker build -t elitebnb-backend:latest .
```

## 5. Load the image into Minikube

```powershell
minikube image load elitebnb-backend:latest
```

The backend Deployment uses `imagePullPolicy: IfNotPresent`, which works well with a local image loaded into Minikube.

## 6. Create namespace

```powershell
kubectl apply -f k8s/namespace.yaml
```

## 7. Create the real Kubernetes Secret locally

Replace each `CHANGE_ME` value before running. This command creates the Secret directly in Kubernetes without saving credentials to a YAML file.

```powershell
kubectl create secret generic elitebnb-backend-secret `
  --namespace elitebnb `
  --from-literal=DB_USERNAME='CHANGE_ME' `
  --from-literal=DB_PASSWORD='CHANGE_ME' `
  --from-literal=PAYSTACK_SECRET_KEY='CHANGE_ME' `
  --from-literal=MAIL_USERNAME='CHANGE_ME' `
  --from-literal=MAIL_PASSWORD='CHANGE_ME' `
  --from-literal=BREVO_API_KEY='CHANGE_ME' `
  --from-literal=CLOUDINARY_CLOUD_NAME='CHANGE_ME' `
  --from-literal=CLOUDINARY_API_KEY='CHANGE_ME' `
  --from-literal=CLOUDINARY_API_SECRET='CHANGE_ME' `
  --from-literal=GOOGLE_CLIENT_ID='CHANGE_ME' `
  --from-literal=GOOGLE_CLIENT_SECRET='CHANGE_ME' `
  --from-literal=ELITEBNB_ADMIN_EMAIL='' `
  --from-literal=ELITEBNB_ADMIN_PASSWORD='' `
  --from-literal=ELITEBNB_ADMIN_FIRST_NAME='' `
  --from-literal=ELITEBNB_ADMIN_LAST_NAME=''
```

To recreate the Secret:

```powershell
kubectl delete secret elitebnb-backend-secret -n elitebnb
```

Then run the `kubectl create secret generic ...` command again.

## 8. Apply ConfigMap

```powershell
kubectl apply -f k8s/configmap.yaml
```

## 9. Deploy PostgreSQL

```powershell
kubectl apply -f k8s/postgres-pvc.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/postgres-service.yaml
```

## 10. Wait for PostgreSQL

```powershell
kubectl wait --for=condition=available deployment/elitebnb-postgres -n elitebnb --timeout=180s
kubectl get pods -n elitebnb
```

## 11. Deploy backend

```powershell
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/backend-service.yaml
```

## 12. Check pods

```powershell
kubectl get pods -n elitebnb -o wide
kubectl describe pod -n elitebnb -l app.kubernetes.io/name=elitebnb-backend
```

## 13. Check services

```powershell
kubectl get services -n elitebnb
```

## 14. Inspect logs

```powershell
kubectl logs -n elitebnb deployment/elitebnb-backend
kubectl logs -n elitebnb deployment/elitebnb-postgres
```

## 15. Access EliteBNB backend

```powershell
minikube service elitebnb-backend -n elitebnb --url
```

Open the returned URL in your browser or use it with `Invoke-RestMethod`.

## 16. Test health

```powershell
$backendUrl = minikube service elitebnb-backend -n elitebnb --url
Invoke-RestMethod "$backendUrl/actuator/health"
```

## 17. Test Prometheus metrics

```powershell
$backendUrl = minikube service elitebnb-backend -n elitebnb --url
Invoke-WebRequest "$backendUrl/actuator/prometheus" | Select-Object -ExpandProperty Content
```

## 18. Restart deployment

```powershell
kubectl rollout restart deployment/elitebnb-backend -n elitebnb
kubectl rollout status deployment/elitebnb-backend -n elitebnb
```

## 19. Delete/recreate backend deployment

```powershell
kubectl delete -f k8s/backend-deployment.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl rollout status deployment/elitebnb-backend -n elitebnb
```

## 20. Tear down EliteBNB Kubernetes resources

```powershell
kubectl delete namespace elitebnb
```

This deletes the local Kubernetes resources, including the PostgreSQL PVC in the `elitebnb` namespace.
